package com.codebosses.flicks.fragments.tvfragments;


import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.budiyev.android.circularprogressbar.CircularProgressBar;
import com.codebosses.flicks.R;
import com.codebosses.flicks.activities.TvShowsDetailActivity;
import com.codebosses.flicks.adapters.tvshowsadapter.TvShowsAdapter;
import com.codebosses.flicks.api.Api;
import com.codebosses.flicks.endpoints.EndpointKeys;
import com.codebosses.flicks.fragments.base.BaseFragment;
import com.codebosses.flicks.pojo.eventbus.EventBusTvShowsClick;
import com.codebosses.flicks.pojo.tvpojo.TvMainObject;
import com.codebosses.flicks.pojo.tvpojo.TvResult;
import com.codebosses.flicks.utils.FontUtils;
import com.codebosses.flicks.utils.ValidUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;


public class FragmentTvShowsOnAir extends BaseFragment {

    //    Android fields....
    @BindView(R.id.textViewErrorMessageTvShowsOnAir)
    TextView textViewError;
    @BindView(R.id.circularProgressBarTvShowsOnAir)
    CircularProgressBar circularProgressBar;
    @BindView(R.id.recyclerViewTvShowsOnAir)
    RecyclerView recyclerViewTvShowsOnAir;
    @BindView(R.id.imageViewErrorTvShowsOnAir)
    AppCompatImageView imageViewError;
    private LinearLayoutManager linearLayoutManager;


    //    Resource fields....
    @BindString(R.string.could_not_get_on_air_tv_shows)
    String couldNotGetTvShows;
    @BindString(R.string.internet_problem)
    String internetProblem;

    //    Font fields....
    private FontUtils fontUtils;

    //    Retrofit fields....
    private Call<TvMainObject> tvMainObjectCall;

    //    Adapter fields....
    private List<TvResult> tvResultArrayList = new ArrayList<>();
    private TvShowsAdapter tvShowsAdapter;
    private int pageNumber = 1, totalPages = 0;


    public FragmentTvShowsOnAir() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_tv_shows_on_air, container, false);
        ButterKnife.bind(this, view);

        EventBus.getDefault().register(this);

//        Setting custom font....
        fontUtils = FontUtils.getFontUtils(getActivity());
        fontUtils.setTextViewBoldFont(textViewError);

        if (getActivity() != null) {
            if (ValidUtils.isNetworkAvailable(getActivity())) {

                tvShowsAdapter = new TvShowsAdapter(getActivity(), tvResultArrayList, EndpointKeys.TV_SHOWS_ON_THE_AIR);
                linearLayoutManager = new LinearLayoutManager(getActivity());
                recyclerViewTvShowsOnAir.setLayoutManager(linearLayoutManager);
                recyclerViewTvShowsOnAir.setItemAnimator(new DefaultItemAnimator());
                recyclerViewTvShowsOnAir.setAdapter(tvShowsAdapter);

                circularProgressBar.setVisibility(View.VISIBLE);
                getOnAirTvShows("en-US", pageNumber);

            } else {
                textViewError.setVisibility(View.VISIBLE);
                imageViewError.setVisibility(View.VISIBLE);
                textViewError.setText(internetProblem);
            }
        }
        recyclerViewTvShowsOnAir.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                boolean isBottomReached = !recyclerView.canScrollVertically(1);
                if (isBottomReached) {
                    pageNumber++;
                    if (pageNumber <= totalPages)
                        getOnAirTvShows("en-US", pageNumber);
                }
            }
        });

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (tvMainObjectCall != null && tvMainObjectCall.isExecuted()) {
            tvMainObjectCall.cancel();
        }
        EventBus.getDefault().unregister(this);
    }

    private void getOnAirTvShows(String language, int pageNumber) {
        tvMainObjectCall = Api.WEB_SERVICE.getTvOnAir(EndpointKeys.THE_MOVIE_DB_API_KEY, language, pageNumber);
        tvMainObjectCall.enqueue(new Callback<TvMainObject>() {
            @Override
            public void onResponse(Call<TvMainObject> call, retrofit2.Response<TvMainObject> response) {
                circularProgressBar.setVisibility(View.INVISIBLE);
                if (response != null && response.isSuccessful()) {
                    TvMainObject tvMainObject = response.body();
                    if (tvMainObject != null) {
                        totalPages = tvMainObject.getTotal_pages();
                        if (tvMainObject.getTotal_results() > 0) {
                            for (int i = 0; i < tvMainObject.getResults().size(); i++) {
                                tvResultArrayList.add(tvMainObject.getResults().get(i));
                                tvShowsAdapter.notifyItemInserted(tvResultArrayList.size() - 1);
                            }
                        }
                    }
                } else {
                    textViewError.setVisibility(View.VISIBLE);
                    textViewError.setText(couldNotGetTvShows);
                    imageViewError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<TvMainObject> call, Throwable error) {
                if (call.isCanceled() || "Canceled".equals(error.getMessage())) {
                    return;
                }
                circularProgressBar.setVisibility(View.INVISIBLE);
                imageViewError.setVisibility(View.VISIBLE);
                textViewError.setVisibility(View.VISIBLE);
                if (error != null) {
                    if (error.getMessage().contains("No address associated with hostname")) {
                        textViewError.setText(internetProblem);
                    } else {
                        textViewError.setText(error.getMessage());
                    }
                } else {
                    textViewError.setText(couldNotGetTvShows);
                }
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void eventBusTvShowsClick(EventBusTvShowsClick eventBusTvShowsClick) {
        if (eventBusTvShowsClick.getTvShowType().equals(EndpointKeys.TV_SHOWS_ON_THE_AIR)) {
            Intent intent = new Intent(getActivity(), TvShowsDetailActivity.class);
            intent.putExtra(EndpointKeys.TV_ID, tvResultArrayList.get(eventBusTvShowsClick.getPosition()).getId());
            intent.putExtra(EndpointKeys.TV_NAME, tvResultArrayList.get(eventBusTvShowsClick.getPosition()).getName());
            intent.putExtra(EndpointKeys.RATING, tvResultArrayList.get(eventBusTvShowsClick.getPosition()).getVote_average());
            startActivity(intent);
        }
    }

}
