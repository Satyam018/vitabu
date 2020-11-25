package ai.elimu.vitabu.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.List;

import ai.elimu.analytics.utils.LearningEventUtil;
import ai.elimu.content_provider.utils.ContentProviderHelper;
import ai.elimu.model.enums.analytics.LearningEventType;
import ai.elimu.model.v2.gson.content.ImageGson;
import ai.elimu.model.v2.gson.content.StoryBookGson;
import ai.elimu.vitabu.BaseApplication;
import ai.elimu.vitabu.BuildConfig;
import ai.elimu.vitabu.R;
import ai.elimu.vitabu.ui.storybook.StoryBookActivity;
import ai.elimu.vitabu.util.SingleClickListener;

public class StoryBooksActivity extends AppCompatActivity {

    private GridLayout storyBooksGridLayout;
    private ProgressBar storyBooksProgressBar;

    private List<StoryBookGson> storyBooks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(getClass().getName(), "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_storybooks);

        storyBooksGridLayout = findViewById(R.id.storyBooksGridLayout);
        storyBooksProgressBar = findViewById(R.id.storybooks_progress_bar);

        // Fetch StoryBooks from the elimu.ai Content Provider (see https://github.com/elimu-ai/content-provider)
        storyBooks = ContentProviderHelper.getStoryBookGsons(getApplicationContext(), BuildConfig.CONTENT_PROVIDER_APPLICATION_ID);
        Log.i(getClass().getName(), "storyBooks.size(): " + storyBooks.size());
    }

    @Override
    protected void onStart() {
        Log.i(getClass().getName(), "onStart");
        super.onStart();

        // Reset the state of the GridLayout
        storyBooksProgressBar.setVisibility(View.VISIBLE);
        storyBooksGridLayout.setVisibility(View.GONE);
        storyBooksGridLayout.removeAllViews();

        ((BaseApplication) getApplication()).getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Create a View for each StoryBook in the list
                for (final StoryBookGson storyBook : storyBooks) {
                    Log.i(getClass().getName(), "storyBook.getId(): " + storyBook.getId());
                    Log.i(getClass().getName(), "storyBook.getTitle(): \"" + storyBook.getTitle() + "\"");
                    Log.i(getClass().getName(), "storyBook.getDescription(): \"" + storyBook.getDescription() + "\"");

                    final View storyBookView = LayoutInflater.from(StoryBooksActivity.this).inflate(R.layout.activity_storybooks_cover_view, storyBooksGridLayout, false);

                    // Fetch Image from the elimu.ai Content Provider (see https://github.com/elimu-ai/content-provider)
                    Log.i(getClass().getName(), "storyBook.getCoverImage(): " + storyBook.getCoverImage());
                    final ImageGson coverImage = ContentProviderHelper.getImageGson(storyBook.getCoverImage().getId(), getApplicationContext(), BuildConfig.CONTENT_PROVIDER_APPLICATION_ID);
                    final ImageView coverImageView = storyBookView.findViewById(R.id.coverImageView);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            File imageFile = new File(Environment.getExternalStorageDirectory() +
                                    "/Android/data/" +
                                    BuildConfig.CONTENT_PROVIDER_APPLICATION_ID +
                                    "/files/" + Environment.DIRECTORY_PICTURES + "/" +
                                    coverImage.getId() + "_r" + coverImage.getRevisionNumber() + "." + coverImage.getImageFormat().toString().toLowerCase());
                            Uri imageFileUri = Uri.fromFile(imageFile);
                            Log.i(getClass().getName(), "imageFileUri: " + imageFileUri);
                            coverImageView.setImageURI(imageFileUri);
                        }
                    });

                    TextView coverTitleTextView = storyBookView.findViewById(R.id.coverTitleTextView);
                    coverTitleTextView.setText(storyBook.getTitle());

                    storyBookView.setOnClickListener(new SingleClickListener() {
                        @Override
                        public void onSingleClick(View v) {
                            Log.i(getClass().getName(), "onClick");

                            Log.i(getClass().getName(), "storyBook.getId(): " + storyBook.getId());
                            Log.i(getClass().getName(), "storyBook.getTitle(): " + storyBook.getTitle());

                            // Report learning event to the Analytics application (https://github.com/elimu-ai/analytics)
                            LearningEventUtil.reportStoryBookLearningEvent(storyBook, LearningEventType.STORYBOOK_OPENED, getApplicationContext(), BuildConfig.ANALYTICS_APPLICATION_ID);

                            Intent intent = new Intent(getApplicationContext(), StoryBookActivity.class);
                            intent.putExtra(StoryBookActivity.EXTRA_KEY_STORYBOOK_ID, storyBook.getId());
                            startActivity(intent);
                        }
                    });

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            storyBooksGridLayout.addView(storyBookView);
                            if (storyBooksGridLayout.getChildCount() == storyBooks.size()) {
                                storyBooksProgressBar.setVisibility(View.GONE);
                                storyBooksGridLayout.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                }
            }
        });
    }
}