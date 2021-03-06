package org.dhis2.data.forms.dataentry.fields.age;

import org.dhis2.data.forms.dataentry.fields.FormViewHolder;
import org.dhis2.data.forms.dataentry.fields.RowAction;
import org.dhis2.databinding.FormAgeCustomBinding;
import org.dhis2.utils.DateUtils;

import io.reactivex.processors.FlowableProcessor;

import static android.text.TextUtils.isEmpty;

/**
 * QUADRAM. Created by frodriguez on 20/03/2018.
 */

public class AgeHolder extends FormViewHolder {

    FormAgeCustomBinding binding;
   /* @NonNull
    private BehaviorProcessor<AgeViewModel> model;*/
//    CompositeDisposable disposable;
    AgeViewModel ageViewModel;

    AgeHolder(FormAgeCustomBinding binding, FlowableProcessor<RowAction> processor) {
        super(binding);
        this.binding = binding;
//        disposable = new CompositeDisposable();
//        model = BehaviorProcessor.create();

      /*  disposable.add(model.subscribe(ageViewModel -> {
                    StringBuilder label = new StringBuilder(ageViewModel.label());
                    if (ageViewModel.mandatory())
                        label.append("*");
                    binding.customAgeview.setLabel(label.toString());
                    if (!isEmpty(ageViewModel.value())) {
                        binding.customAgeview.setInitialValue(ageViewModel.value());
                    }

                    if (ageViewModel.warning() != null)
                        binding.customAgeview.setWarningOrError(ageViewModel.warning());
                    else if (ageViewModel.error() != null)
                        binding.customAgeview.setWarningOrError(ageViewModel.error());
                    else
                        binding.customAgeview.setWarningOrError(null);

                    binding.customAgeview.setEditable(ageViewModel.editable());

                    binding.executePendingBindings();
                },
                Timber::d));*/

        binding.customAgeview.setAgeChangedListener(ageDate -> {
                    if (ageViewModel.value() == null || !ageViewModel.value().equals(DateUtils.databaseDateFormat().format(ageDate)))
                        processor.onNext(RowAction.create(ageViewModel.uid(), DateUtils.databaseDateFormat().format(ageDate)));
                }
        );
    }


    public void update(AgeViewModel ageViewModel) {
//        model.onNext(viewModel);
        this.ageViewModel = ageViewModel;

        StringBuilder label = new StringBuilder(ageViewModel.label());
        if (ageViewModel.mandatory())
            label.append("*");
        binding.customAgeview.setLabel(label.toString());
        if (!isEmpty(ageViewModel.value())) {
            binding.customAgeview.setInitialValue(ageViewModel.value());
        }

        if (ageViewModel.warning() != null)
            binding.customAgeview.setWarningOrError(ageViewModel.warning());
        else if (ageViewModel.error() != null)
            binding.customAgeview.setWarningOrError(ageViewModel.error());
        else
            binding.customAgeview.setWarningOrError(null);

        binding.customAgeview.setEditable(ageViewModel.editable());

        binding.executePendingBindings();

    }

    @Override
    public void dispose() {
//        disposable.clear();
    }
}
