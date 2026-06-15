package com.safetour.ai.ui;

import androidx.annotation.NonNull;
import androidx.navigation.ActionOnlyNavDirections;
import androidx.navigation.NavDirections;
import com.safetour.ai.R;

public class SetupFragmentDirections {
  private SetupFragmentDirections() {
  }

  @NonNull
  public static NavDirections actionSetupToHome() {
    return new ActionOnlyNavDirections(R.id.action_setup_to_home);
  }
}
