package com.safetour.ai.ui;

import androidx.annotation.NonNull;
import androidx.navigation.ActionOnlyNavDirections;
import androidx.navigation.NavDirections;
import com.safetour.ai.R;

public class LoginFragmentDirections {
  private LoginFragmentDirections() {
  }

  @NonNull
  public static NavDirections actionLoginToHome() {
    return new ActionOnlyNavDirections(R.id.action_login_to_home);
  }

  @NonNull
  public static NavDirections actionLoginToSetup() {
    return new ActionOnlyNavDirections(R.id.action_login_to_setup);
  }
}
