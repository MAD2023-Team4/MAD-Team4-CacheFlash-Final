package sg.edu.np.mad.madasgcacheflash;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;



public class StudyStreakPage extends AppCompatActivity {
    private String username;
    private DatabaseReference streakStatus;
    private Date startDate;
    private DatabaseReference totalDaysRef;
    private int currentStreak;

    private ImageView medal10DaysOutline;
    private ImageView medal10DaysColored;
    private ImageView medal20DaysOutline;
    private ImageView medal20DaysColored;
    private ImageView medal30DaysOutline;
    private ImageView medal30DaysColored;

    private DatabaseReference medalStatusRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_streak_page);
        Intent intent = getIntent();
        username = intent.getStringExtra("Username");
        Log.d("StudyStreakPage", "Username: " + username);
        TextView usernameTextView = findViewById(R.id.textView20);
        usernameTextView.setText(username);
        streakStatus = FirebaseDatabase.getInstance().getReference().child("users").child(username).child("Streak Status");
        totalDaysRef = FirebaseDatabase.getInstance().getReference().child("users").child(username).child("Total Days");
        medalStatusRef = FirebaseDatabase.getInstance().getReference().child("users").child(username).child("MedalStatus");

        TextView streakText = findViewById(R.id.textView22);
        TextView totalDaysText = findViewById(R.id.textView21);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        TextView textViewBackToProfile = findViewById(R.id.backToProfile);

        medal10DaysOutline = findViewById(R.id.medal10DaysOutline);
        medal10DaysColored = findViewById(R.id.medal10DaysColored);
        medal20DaysOutline = findViewById(R.id.medal20DaysOutline);
        medal20DaysColored = findViewById(R.id.medal20DaysColored);
        medal30DaysOutline = findViewById(R.id.medal30DaysOutline);
        medal30DaysColored = findViewById(R.id.medal30DaysColored);


        // Set the colored medals as invisible initially
        medal10DaysColored.setVisibility(View.INVISIBLE);
        medal20DaysColored.setVisibility(View.INVISIBLE);
        medal30DaysColored.setVisibility(View.INVISIBLE);


        textViewBackToProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentProfile = new Intent(StudyStreakPage.this,Profile.class);
                intentProfile.putExtra("Username",username);
                startActivity(intentProfile);
            }
        });

        // Retrieve the current date
        Date currentDate = new Date();

// Check if the user has a previous streak recorded
        streakStatus.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Previous streak exists
                    startDate = dataSnapshot.child("startDate").getValue(Date.class);
                    currentStreak = dataSnapshot.child("currentStreak").getValue(Integer.class);
                    streakText.setText(String.valueOf(currentStreak));
                    progressBar.setProgress(currentStreak);
                    currentStreak = 10;

                    unlockMedal(currentStreak);
                    medalStatusRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.hasChild("10DayMedal") && dataSnapshot.hasChild("20DayMedal") && dataSnapshot.hasChild("30DayMedal")) {
                                boolean is10DayMedalUnlocked = dataSnapshot.child("10DayMedal").getValue(Boolean.class);
                                boolean is20DayMedalUnlocked = dataSnapshot.child("20DayMedal").getValue(Boolean.class);
                                boolean is30DayMedalUnlocked = dataSnapshot.child("30DayMedal").getValue(Boolean.class);

                                // Update the visibility of the medals based on the unlocked status
                                if (is10DayMedalUnlocked) {
                                    medal10DaysOutline.setVisibility(View.INVISIBLE);
                                    medal10DaysColored.setVisibility(View.VISIBLE);
                                }
                                if (is20DayMedalUnlocked) {
                                    medal20DaysOutline.setVisibility(View.INVISIBLE);
                                    medal20DaysColored.setVisibility(View.VISIBLE);
                                }
                                if (is30DayMedalUnlocked) {
                                    medal30DaysOutline.setVisibility(View.INVISIBLE);
                                    medal30DaysColored.setVisibility(View.VISIBLE);
                                }
                            } else {
                                // Medals not unlocked yet
                                medal10DaysOutline.setVisibility(View.VISIBLE);
                                medal10DaysColored.setVisibility(View.INVISIBLE);
                                medal20DaysOutline.setVisibility(View.VISIBLE);
                                medal20DaysColored.setVisibility(View.INVISIBLE);
                                medal30DaysOutline.setVisibility(View.VISIBLE);
                                medal30DaysColored.setVisibility(View.INVISIBLE);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            // Error retrieving medal status from Firebase
                            Log.e("MedalStatus", "Error getting medal status: " + databaseError.getMessage());
                        }
                    });

                } else {
                    // No previous streak recorded
                    streakText.setText("0");

                    // Check if the "startDate" child node exists
                    if (!dataSnapshot.hasChild("startDate")) {
                        // "startDate" child node does not exist, create it based on the current date
                        streakStatus.child("startDate").setValue(currentDate);
                    }
                    if (!dataSnapshot.hasChild("currentStreak")){
                        streakStatus.child("currentStreak").setValue(0);
                    }
                }

                // Calculate and display the total number of days
                long totalDays = calculateTotalDays(startDate, currentDate);
                totalDaysText.setText(String.valueOf(totalDays));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Error retrieving streak data from the Realtime Database
                Log.e("Streak", "Error getting streak data: " + databaseError.getMessage());
            }
        });


        progressBar.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int remainingDays = 10 - currentStreak;
                String message = remainingDays + " days left till next medal";
                Snackbar.make(v, message, Snackbar.LENGTH_SHORT).show();
                return true;
            }
        });

        // Retrieve the LineChartView from your layout
        /*LineChartView lineChartView = findViewById(R.id.chart);

// Create a list of axis values for X-axis labels (e.g., days of the week)
        List<AxisValue> axisValues = new ArrayList<>();
        axisValues.add(new AxisValue(0).setLabel("Mon"));
        axisValues.add(new AxisValue(1).setLabel("Tue"));
        axisValues.add(new AxisValue(2).setLabel("Wed"));
        axisValues.add(new AxisValue(3).setLabel("Thu"));
        axisValues.add(new AxisValue(4).setLabel("Fri"));
        axisValues.add(new AxisValue(5).setLabel("Sat"));
        axisValues.add(new AxisValue(6).setLabel("Sun"));

// Create a list of data points for the line chart
        List<PointValue> values = new ArrayList<>();
        values.add(new PointValue(0, 5));  // Monday
        values.add(new PointValue(1, 3));  // Tuesday
        values.add(new PointValue(2, 6));  // Wednesday
        values.add(new PointValue(3, 4));  // Thursday
        values.add(new PointValue(4, 8));  // Friday
        values.add(new PointValue(5, 2));  // Saturday
        values.add(new PointValue(6, 7));  // Sunday

// Create a line from the values and customize it
        Line line = new Line(values)
                .setColor(ChartUtils.COLOR_BLUE)
                .setCubic(true)
                .setPointRadius(4)
                .setStrokeWidth(2)
                .setHasLabels(true);

// Create a list of lines (in case you want to add multiple lines)
        List<Line> lines = new ArrayList<>();
        lines.add(line);

// Create the chart data and set the axis labels
        LineChartData chartData = new LineChartData(lines);
        chartData.setAxisXBottom(new Axis(axisValues));
        chartData.setAxisYLeft(new Axis().setHasLines(true));

// Set the chart configuration and update the chart view
        lineChartView.setLineChartData(chartData);*/
    }


    private long calculateTotalDays(Date startDate, Date currentDate) {
        if (startDate != null) {
            long startTime = startDate.getTime();
            long currentTime = currentDate.getTime();
            long differenceMillis = currentTime - startTime;
            return TimeUnit.MILLISECONDS.toDays(differenceMillis);
        }
        return 0;
    }
    private void unlockMedal(int streak) {
        if (streak == 10) {
            medalStatusRef.child("10DayMedal").setValue(true);
        } else if (streak == 20) {
            medalStatusRef.child("20DayMedal").setValue(true);
        } else if (streak == 30) {
            medalStatusRef.child("30DayMedal").setValue(true);
        }
    }

}
