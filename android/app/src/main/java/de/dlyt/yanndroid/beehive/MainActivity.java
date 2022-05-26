package de.dlyt.yanndroid.beehive;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import dev.oneuiproject.oneui.dialog.ProgressDialog;
import dev.oneuiproject.oneui.layout.DrawerLayout;
import dev.oneuiproject.oneui.utils.internal.ReflectUtils;

@SuppressLint("RestrictedApi")
public class MainActivity extends AppCompatActivity {

    private Context mContext;
    private DrawerLayout drawerLayout;
    private RecyclerView drawerListView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout dashboardLayout, errorLayout;
    private AppCompatTextView hive_temp, hive_humid, out_temp, out_humid, last_updated;
    private LineChart temp_chart, humid_chart;

    private DrawerListAdapter listAdapter;
    private List<ServerItem> servers;
    private int currentServerIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        loadServers();
        initLayout();
        loadServerData(currentServerIndex, true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveServers();
    }

    private void loadServers() {
        SharedPreferences sp = getSharedPreferences("server_list", MODE_PRIVATE);
        servers = new Gson().fromJson(sp.getString("list", null), new TypeToken<List<ServerItem>>() {
        }.getType());
        if (servers == null) servers = new ArrayList<>();
        currentServerIndex = sp.getInt("last_index", -1);
    }

    private void saveServers() {
        SharedPreferences sp = getSharedPreferences("server_list", MODE_PRIVATE);
        sp.edit().putString("list", new Gson().toJson(servers)).putInt("last_index", currentServerIndex).apply();
    }

    private void newServerDialog() {
        View view = getLayoutInflater().inflate(R.layout.new_hive_dialog_layout, null);
        AppCompatEditText[] editTexts = new AppCompatEditText[]{view.findViewById(R.id.dialog_name), view.findViewById(R.id.dialog_url)};

        AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setTitle(R.string.new_hive)
                .setView(view)
                .setPositiveButton(R.string.add, (dialogInterface, i) -> {
                    servers.add(new ServerItem(editTexts[0].getText().toString(), editTexts[1].getText().toString()));
                    currentServerIndex = servers.size() - 1;
                    listAdapter.notifyItemInserted(currentServerIndex);
                    listAdapter.setSelectedItem(currentServerIndex);
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(dev.oneuiproject.oneui.R.color.oui_functional_green_color));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        for (AppCompatEditText editText : editTexts) {
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(Arrays.stream(editTexts).allMatch(appCompatEditText -> !appCompatEditText.getText().toString().isEmpty()));
                }
            });
        }
    }

    private void deleteServerDialog(int index) {
        AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setTitle(servers.get(index).name)
                .setMessage(R.string.delete_hive_msg)
                .setPositiveButton(R.string.delete, (dialogInterface, i) -> {
                    servers.remove(index);
                    listAdapter.notifyItemRemoved(index);
                    if (index == currentServerIndex) {
                        listAdapter.setSelectedItem(index == 0 && servers.size() > 0 ? 0 : index - 1);
                    } else if (index < currentServerIndex) {
                        currentServerIndex -= 1;
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(dev.oneuiproject.oneui.R.color.oui_functional_red_color));
    }

    private void initLayout() {
        drawerLayout = findViewById(R.id.drawerLayout);
        drawerListView = findViewById(R.id.drawer_list_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        dashboardLayout = findViewById(R.id.dashboard_layout);
        errorLayout = findViewById(R.id.error_layout);

        hive_temp = findViewById(R.id.hive_temp);
        hive_humid = findViewById(R.id.hive_humid);
        out_temp = findViewById(R.id.outside_temp);
        out_humid = findViewById(R.id.outside_humid);
        last_updated = findViewById(R.id.last_time_updated);

        temp_chart = findViewById(R.id.temp_chart);
        humid_chart = findViewById(R.id.humid_chart);

        drawerListView.setLayoutManager(new LinearLayoutManager(this));
        drawerListView.setAdapter(listAdapter = new DrawerListAdapter());
        drawerListView.setItemAnimator(null);
        drawerListView.seslSetLastRoundedCorner(false);

        drawerLayout.setDrawerButtonIcon(getDrawable(dev.oneuiproject.oneui.R.drawable.ic_oui_ab_add));
        drawerLayout.setDrawerButtonTooltip(getString(R.string.new_hive));
        drawerLayout.setDrawerButtonOnClickListener(view -> newServerDialog());
        drawerLayout.getAppBarLayout().addOnOffsetChangedListener((layout, verticalOffset) -> {
            int totalScrollRange = layout.getTotalScrollRange();
            int inputMethodWindowVisibleHeight = (int) ReflectUtils.genericInvokeMethod(InputMethodManager.class, mContext.getSystemService(INPUT_METHOD_SERVICE), "getInputMethodWindowVisibleHeight");
            if (errorLayout != null) {
                if (totalScrollRange != 0) {
                    errorLayout.setTranslationY(((float) (Math.abs(verticalOffset) - totalScrollRange)) / 2.0f);
                } else {
                    errorLayout.setTranslationY(((float) (Math.abs(verticalOffset) - inputMethodWindowVisibleHeight)) / 2.0f);
                }
            }
        });
        swipeRefreshLayout.setOnRefreshListener(() -> loadServerData(currentServerIndex, false));

        initChart(temp_chart);
        initChart(humid_chart);
    }

    private void initChart(LineChart chart) {
        chart.setDescription(null);
        chart.getXAxis().setDrawGridLines(false);
        chart.setScaleYEnabled(false);
        int textColor = getColor(dev.oneuiproject.oneui.R.color.oui_primary_text_color);
        chart.getXAxis().setTextColor(textColor);
        chart.getAxisLeft().setTextColor(textColor);
        chart.getAxisRight().setTextColor(textColor);
        chart.getLegend().setTextColor(textColor);
    }

    @SuppressLint("StaticFieldLeak")
    private void loadServerData(int position, boolean showPD) {
        if (position == -1 || servers.size() <= position) {
            showServerData(null);
            return;
        }
        ServerItem serverItem = servers.get(position);
        drawerLayout.setExpandedSubtitle(serverItem.name);
        drawerLayout.setTitle(getTitle(), serverItem.name);

        if (serverItem.url == null) {
            showServerData(null);
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_CIRCLE);
        if (showPD) progressDialog.show();

        new AsyncTask<String, Void, ServerData>() {
            @Override
            protected ServerData doInBackground(String... url) {
                if (url[0].equals("debug"))
                    return new Random().nextInt(4) != 0 ? new ServerData(mContext).testData() : null;

                try {
                    URLConnection connection = new URL(url[0]).openConnection();
                    connection.connect();

                    InputStream inputStream = connection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) return null;

                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) buffer.append(line).append("\n");

                    if (buffer.length() == 0) return null;
                    return new ServerData(mContext).parseJson(buffer.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(ServerData response) {
                progressDialog.dismiss();
                swipeRefreshLayout.setRefreshing(false);
                showServerData(response);
            }
        }.execute(serverItem.url);
    }

    @SuppressLint("StringFormatInvalid")
    private void showServerData(ServerData serverData) {
        if (serverData == null) {
            dashboardLayout.setVisibility(View.GONE);
            errorLayout.setVisibility(View.VISIBLE);
            boolean no_servers = servers.isEmpty();
            ((AppCompatImageView) findViewById(R.id.error_icon)).setImageResource(no_servers ? dev.oneuiproject.oneui.R.drawable.ic_oui_creatures_outline : dev.oneuiproject.oneui.R.drawable.ic_oui_no_network);
            ((AppCompatTextView) findViewById(R.id.error_title)).setText(no_servers ? R.string.no_servers : R.string.no_connection);
            ((AppCompatTextView) findViewById(R.id.error_message)).setText(no_servers ? R.string.no_servers_message : R.string.no_connection_message);
            if (no_servers) {
                drawerLayout.setTitle(getString(R.string.app_name));
                drawerLayout.setExpandedSubtitle(null);
            }
        } else {
            dashboardLayout.setVisibility(View.VISIBLE);
            errorLayout.setVisibility(View.GONE);

            hive_temp.setText(getString(R.string.temperature_value, serverData.lTempHive));
            hive_humid.setText(getString(R.string.humidity_value, serverData.lHumidHive));
            out_temp.setText(getString(R.string.temperature_value, serverData.lTempOut));
            out_humid.setText(getString(R.string.humidity_value, serverData.lHumidOut));
            last_updated.setText(serverData.lDate);

            temp_chart.setData(serverData.temp_data);
            temp_chart.animateX(1000);
            humid_chart.setData(serverData.humid_data);
            humid_chart.animateX(1000);
        }
    }

    private class DrawerListAdapter extends RecyclerView.Adapter<DrawerListAdapter.DrawerListViewHolder> {
        @NonNull
        @Override
        public DrawerListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new DrawerListViewHolder(LayoutInflater.from(mContext).inflate(R.layout.drawer_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull DrawerListViewHolder holder, int position) {
            holder.mTitleView.setText(servers.get(position).name);
            holder.mSubTitleView.setText(servers.get(position).url);
            holder.deleteView.setOnClickListener(view -> deleteServerDialog(holder.getBindingAdapterPosition()));
            holder.itemView.setSelected(position == currentServerIndex);
            holder.itemView.setOnClickListener(v -> setSelectedItem(holder.getBindingAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return servers.size();
        }

        public void setSelectedItem(int position) {
            loadServerData(currentServerIndex = position, true);
            notifyItemRangeChanged(0, getItemCount());
            drawerLayout.setDrawerOpen(false, true);
        }

        public class DrawerListViewHolder extends RecyclerView.ViewHolder {
            private TextView mTitleView;
            private TextView mSubTitleView;
            private AppCompatImageView deleteView;

            public DrawerListViewHolder(@NonNull View itemView) {
                super(itemView);
                mTitleView = itemView.findViewById(R.id.drawer_item_title);
                mSubTitleView = itemView.findViewById(R.id.drawer_item_subtitle);
                deleteView = itemView.findViewById(R.id.drawer_item_delete);
            }
        }
    }

}