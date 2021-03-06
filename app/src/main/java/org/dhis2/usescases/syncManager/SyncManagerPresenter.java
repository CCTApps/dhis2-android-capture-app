package org.dhis2.usescases.syncManager;

import android.content.Context;
import android.content.SharedPreferences;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobTrigger;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

import org.dhis2.data.metadata.MetadataRepository;
import org.dhis2.data.service.SyncDataService;
import org.dhis2.data.service.SyncMetadataService;
import org.dhis2.data.tuples.Pair;
import org.dhis2.usescases.login.LoginActivity;
import org.dhis2.utils.Constants;
import org.hisp.dhis.android.core.D2;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static org.dhis2.usescases.syncManager.SyncManagerFragment.TAG_DATA_NOW;
import static org.dhis2.usescases.syncManager.SyncManagerFragment.TAG_META_NOW;

/**
 * QUADRAM. Created by lmartin on 21/03/2018.
 */

public class SyncManagerPresenter implements SyncManagerContracts.Presenter {

    private static final int DATA_RESOURCE = 16;  //Events Resource
    private static final int META_DATA_RESOURCE = 1; // User Resource
    private final D2 d2;

    private MetadataRepository metadataRepository;
    private FirebaseJobDispatcher dispatcher;
    private CompositeDisposable compositeDisposable;
    private SyncManagerContracts.View view;

    SyncManagerPresenter(MetadataRepository metadataRepository, FirebaseJobDispatcher dispatcher, D2 d2) {
        this.metadataRepository = metadataRepository;
        this.dispatcher = dispatcher;
        this.d2 = d2;
    }

    @Override
    public void init(SyncManagerContracts.View view) {
        this.view = view;
        this.compositeDisposable = new CompositeDisposable();

        compositeDisposable.add(
                metadataRepository.getDownloadedData()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                view.setSyncData(),
                                Timber::e
                        )
        );
    }

    @Override
    public void syncData(int seconds, String scheduleTag) {

        if (seconds == 0)
            dispatcher.cancel(scheduleTag);
        else {
            Job dataJob;

            dataJob = dispatcher.newJobBuilder()
                    .setService(SyncDataService.class)
                    .setTag(scheduleTag)
                    .setRecurring(true)
                    .setLifetime(Lifetime.FOREVER)
                    .setTrigger(Trigger.executionWindow(seconds, seconds + 60))
//                    .setReplaceCurrent(true)
                    .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                    .setConstraints(
                            Constraint.ON_ANY_NETWORK
                    )
                    .build();

            dispatcher.mustSchedule(dataJob);
        }
    }

    @Override
    public void syncMeta(int seconds, String scheduleTag) {

        if (seconds == 0)
            dispatcher.cancel(scheduleTag);
        else {
            Job metaJob;

            boolean isRecurring = false;
            JobTrigger trigger = Trigger.NOW;

            if (seconds != 0) {
                isRecurring = true;
                trigger = Trigger.executionWindow(seconds, seconds + 60);
            }

            metaJob = dispatcher.newJobBuilder()
                    .setService(SyncMetadataService.class)
                    .setTag(scheduleTag)
                    .setRecurring(isRecurring)
                    .setLifetime(Lifetime.FOREVER)
                    .setTrigger(trigger)
//                    .setReplaceCurrent(true)
                    .setConstraints(
                            Constraint.ON_ANY_NETWORK
                    )
                    .build();
            dispatcher.mustSchedule(metaJob);
        }
    }

    @Override
    public void syncData() {
        Job dataJob;

        dataJob = dispatcher.newJobBuilder()
                .setService(SyncDataService.class)
                .setTag(TAG_DATA_NOW)
                .setRecurring(false)
                .setReplaceCurrent(true)
                .setConstraints(
                        Constraint.ON_ANY_NETWORK
                )
                .build();

        dispatcher.mustSchedule(dataJob);
    }

    @Override
    public void syncMeta() {
        Job dataJob;

        dataJob = dispatcher.newJobBuilder()
                .setService(SyncMetadataService.class)
                .setTag(TAG_META_NOW)
                .setRecurring(false)
                .setReplaceCurrent(true)
                .setConstraints(
                        Constraint.ON_ANY_NETWORK
                )
                .build();
        dispatcher.mustSchedule(dataJob);
    }

    @Override
    public void disponse() {
        compositeDisposable.clear();
    }

    @Override
    public void resetSyncParameters() {
        SharedPreferences prefs = view.getAbstracContext().getSharedPreferences(
                Constants.SHARE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt(Constants.EVENT_MAX, Constants.EVENT_MAX_DEFAULT);
        editor.putInt(Constants.TEI_MAX, Constants.TEI_MAX_DEFAULT);
        editor.putBoolean(Constants.LIMIT_BY_ORG_UNIT, false);

        editor.apply();

        compositeDisposable.add(
                Observable.just(Pair.create(Constants.EVENT_MAX_DEFAULT, Constants.TEI_MAX_DEFAULT))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                view.setSyncData(),
                                Timber::d
                        )
        );

    }

    @Override
    public void onWipeData() {

        view.wipeDatabase();

    }

    @Override
    public void wipeDb() {
        try {
            dispatcher.cancelAll();
            d2.wipeDB().call();

            // clearing cache data
            deleteDir(view.getAbstracContext().getCacheDir());

            view.getAbstracContext().getSharedPreferences().edit().clear().apply();

            view.startActivity(LoginActivity.class, null, true, true, null);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }
}
