package com.safetour.ai.ui;

import androidx.annotation.NonNull;
import androidx.navigation.ActionOnlyNavDirections;
import androidx.navigation.NavDirections;
import com.safetour.ai.R;

public class SplashFragmentDirections {
  private SplashFragmentDirections() {
  }

  @NonNull
  public static NavDirections actionSplashToLogin() {
    return new ActionOnlyNavDirections(R.id.action_splash_to_login);
  }
}
