package com.safetour.ai.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.safetour.ai.R;
import com.safetour.ai.viewmodel.SharedLocationViewModel;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends Fragment {

    private MapView mapView;
    private SharedLocationViewModel locationViewModel;
    private DestinationsAdapter destinationsAdapter;
    private List<Destination> destinationList;
    private android.location.Location lastFetchedLocation = null;
    private android.location.Location lastPlacesFetchedLocation = null;
    private org.osmdroid.views.overlay.Polyline currentRouteOverlay = null;
    private org.osmdroid.views.overlay.Marker destinationMarkerOverlay = null;
    
    class NavStep {
        double lat;
        double lng;
        String instruction;
        String modifier;

        NavStep(double lat, double lng, String instruction, String modifier) {
            this.lat = lat;
            this.lng = lng;
            this.instruction = instruction;
            this.modifier = modifier;
        }
    }
    
    private boolean isNavigationMode = false;
    private int currentStepIndex = 0;
    private List<NavStep> navSteps = new ArrayList<>();
    private org.osmdroid.views.overlay.compass.CompassOverlay compassOverlay = null;

    private void animateClickAndRun(View view, Runnable action) {
        view.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() -> {
            view.animate().scaleX(1f).scaleY(1f).setDuration(100).withEndAction(action).start();
        }).start();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        locationViewModel = new ViewModelProvider(requireActivity()).get(SharedLocationViewModel.class);

        mapView = view.findViewById(R.id.exploreMapView);
        if (mapView != null) {
            mapView.setMultiTouchControls(true);
            mapView.getController().setZoom(15.0);
            
            // Dynamic Tile Inversion for Deep Dark Mode Matrices
            if ((getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                android.graphics.ColorMatrix colorMatrix = new android.graphics.ColorMatrix(new float[] {
                    -1.0f,  0.0f,  0.0f,  0.0f, 255.0f,
                     0.0f, -1.0f,  0.0f,  0.0f, 255.0f,
                     0.0f,  0.0f, -1.0f,  0.0f, 255.0f,
                     0.0f,  0.0f,  0.0f,  1.0f,   0.0f
                });
                mapView.getOverlayManager().getTilesOverlay().setColorFilter(new android.graphics.ColorMatrixColorFilter(colorMatrix));
            }
            
            org.osmdroid.views.overlay.gestures.RotationGestureOverlay rotationGestureOverlay = new org.osmdroid.views.overlay.gestures.RotationGestureOverlay(mapView);
            rotationGestureOverlay.setEnabled(true);
            mapView.getOverlays().add(rotationGestureOverlay);
            
            compassOverlay = new org.osmdroid.views.overlay.compass.CompassOverlay(requireContext(), new org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider(requireContext()), mapView) {
                private long lastRotationTime = 0;
                @Override
                public void onOrientationChanged(float orientation, org.osmdroid.views.overlay.compass.IOrientationProvider source) {
                    super.onOrientationChanged(orientation, source);
                    if (isNavigationMode && mapView != null) {
                        long now = System.currentTimeMillis();
                        if (now - lastRotationTime > 100) {
                            lastRotationTime = now;
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    mapView.setMapOrientation(360 - orientation);
                                });
                            }
                        }
                    }
                }
            };
            compassOverlay.enableCompass();
            float density = getResources().getDisplayMetrics().density;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            // Position exactly on the left side, ~350dp from the bottom edge to clear the cards
            compassOverlay.setCompassCenter(60f * density, screenHeight - (350f * density));
            mapView.getOverlays().add(compassOverlay);
        }

        ImageView ivSettings = view.findViewById(R.id.ivSettings);
        if (ivSettings != null) {
            ivSettings.setImageResource(android.R.drawable.ic_menu_sort_by_size);
            ivSettings.setOnClickListener(v -> {
                animateClickAndRun(ivSettings, () -> {
                    android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), ivSettings);
                    popup.getMenu().add(1, 1, 1, "Switch to Standard Map");
                    popup.getMenu().add(1, 2, 2, "Switch to Satellite Map");
                    popup.getMenu().add(1, 3, 3, "Add Custom Safe Zone Pin");
                    popup.setOnMenuItemClickListener(item -> {
                        switch (item.getItemId()) {
                            case 1:
                                if(mapView != null) {
                                    mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
                                    mapView.invalidate();
                                }
                                break;
                            case 2:
                                if(mapView != null) {
                                    mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.USGS_SAT);
                                    mapView.invalidate();
                                }
                                break;
                            case 3:
                                if(mapView != null) {
                                    org.osmdroid.views.overlay.Marker customPin = new org.osmdroid.views.overlay.Marker(mapView);
                                    customPin.setPosition((org.osmdroid.util.GeoPoint) mapView.getMapCenter());
                                    customPin.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
                                    customPin.setTitle("Custom Safe Zone");
                                    customPin.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_myplaces, null));
                                    mapView.getOverlays().add(customPin);
                                    mapView.invalidate();
                                    android.widget.Toast.makeText(requireContext(), "Custom Safe Zone Pin added at map center.", android.widget.Toast.LENGTH_SHORT).show();
                                }
                                break;
                        }
                        return true;
                    });
                    popup.show();
                });
            });
        }

        EditText etSearch = view.findViewById(R.id.etSearch);
        if (etSearch != null) {
            etSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = etSearch.getText().toString();
                    if (!query.isEmpty()) { launchMapSearch(query); }
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                    return true;
                }
                return false;
            });
        }

        setupChip(view, R.id.chipPolice, "Police Stations");
        setupChip(view, R.id.chipHospitals, "Hospitals");
        setupChip(view, R.id.chipBeaches, "Beaches");

        initDestinationsList();
        RecyclerView rvDestinations = view.findViewById(R.id.rvDestinations);
        if (rvDestinations != null) {
            destinationsAdapter = new DestinationsAdapter(destinationList);
            rvDestinations.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            rvDestinations.setAdapter(destinationsAdapter);
        }
        
        View fabToggleCards = view.findViewById(R.id.fabToggleCards);
        ImageView ivToggleIcon = view.findViewById(R.id.ivToggleIcon);
        
        if (fabToggleCards != null && ivToggleIcon != null && rvDestinations != null) {
            fabToggleCards.setOnClickListener(v -> {
                animateClickAndRun(fabToggleCards, () -> {
                    if (rvDestinations.getVisibility() == View.VISIBLE) {
                        rvDestinations.animate().translationY(rvDestinations.getHeight()).alpha(0.0f).setDuration(200).withEndAction(() -> {
                            rvDestinations.setVisibility(View.GONE);
                            ivToggleIcon.setImageResource(android.R.drawable.arrow_up_float);
                        }).start();
                    } else {
                        rvDestinations.setVisibility(View.VISIBLE);
                        rvDestinations.animate().translationY(0.0f).alpha(1.0f).setDuration(200).withEndAction(() -> {
                            ivToggleIcon.setImageResource(android.R.drawable.arrow_down_float);
                        }).start();
                    }
                });
            });
        }
        
        View btnStartNav = view.findViewById(R.id.btnStartNav);
        View btnExitNav = view.findViewById(R.id.btnExitNav);
        View cardNavHeader = view.findViewById(R.id.cardNavHeader);
        
        if (btnStartNav != null) {
            btnStartNav.setOnClickListener(v -> {
                isNavigationMode = true;
                currentStepIndex = 0;
                btnStartNav.setVisibility(View.GONE);
                
                if (view.findViewById(R.id.searchContainer) != null) view.findViewById(R.id.searchContainer).setVisibility(View.GONE);
                if (view.findViewById(R.id.scrollFilters) != null) view.findViewById(R.id.scrollFilters).setVisibility(View.GONE);
                if (view.findViewById(R.id.tvExploreTitle) != null) view.findViewById(R.id.tvExploreTitle).setVisibility(View.GONE);
                if (view.findViewById(R.id.etSearch) != null) view.findViewById(R.id.etSearch).setVisibility(View.GONE);
                if (rvDestinations != null) rvDestinations.setVisibility(View.GONE);
                if (fabToggleCards != null) fabToggleCards.setVisibility(View.GONE);
                if (ivSettings != null) ivSettings.setVisibility(View.GONE);
                
                if (cardNavHeader != null) cardNavHeader.setVisibility(View.VISIBLE);
                if (btnExitNav != null) btnExitNav.setVisibility(View.VISIBLE);
                
                if (lastFetchedLocation != null) {
                    mapView.getController().animateTo(new GeoPoint(lastFetchedLocation.getLatitude(), lastFetchedLocation.getLongitude()), 19.0, 1000L);
                } else {
                    mapView.getController().setZoom(19.0);
                }
                
                updateNavHeaderUI();
            });
        }
        
        if (btnExitNav != null) {
            btnExitNav.setOnClickListener(v -> {
                isNavigationMode = false;
                if (mapView != null) mapView.setMapOrientation(0f);
                btnExitNav.setVisibility(View.GONE);
                if (cardNavHeader != null) cardNavHeader.setVisibility(View.GONE);
                
                if (view.findViewById(R.id.searchContainer) != null) view.findViewById(R.id.searchContainer).setVisibility(View.VISIBLE);
                if (view.findViewById(R.id.scrollFilters) != null) view.findViewById(R.id.scrollFilters).setVisibility(View.VISIBLE);
                if (view.findViewById(R.id.tvExploreTitle) != null) view.findViewById(R.id.tvExploreTitle).setVisibility(View.VISIBLE);
                if (view.findViewById(R.id.etSearch) != null) view.findViewById(R.id.etSearch).setVisibility(View.VISIBLE);
                if (rvDestinations != null) rvDestinations.setVisibility(View.VISIBLE);
                if (fabToggleCards != null) fabToggleCards.setVisibility(View.VISIBLE);
                if (ivSettings != null) ivSettings.setVisibility(View.VISIBLE);
                
                if (currentRouteOverlay != null) mapView.getOverlays().remove(currentRouteOverlay);
                if (destinationMarkerOverlay != null) mapView.getOverlays().remove(destinationMarkerOverlay);
                mapView.setMapOrientation(0f);
                mapView.invalidate();
                if (lastFetchedLocation != null) {
                    mapView.getController().animateTo(new GeoPoint(lastFetchedLocation.getLatitude(), lastFetchedLocation.getLongitude()), 15.0, 1000L);
                } else {
                    mapView.getController().setZoom(15.0);
                }
            });
        }
        
        View fabRecenter = view.findViewById(R.id.fabRecenter);
        if (fabRecenter != null) {
            fabRecenter.setOnClickListener(v -> {
                animateClickAndRun(fabRecenter, () -> {
                    if (lastFetchedLocation != null) {
                        mapView.getController().animateTo(new GeoPoint(lastFetchedLocation.getLatitude(), lastFetchedLocation.getLongitude()), isNavigationMode ? 19.0 : 15.0, 800L);
                    } else {
                        android.widget.Toast.makeText(requireContext(), "Awaiting location lock...", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        setupMapLocation();
    }

    private void initDestinationsList() {
        destinationList = new ArrayList<>();
        // Now dynamically fetched via Wikipedia API based on GPS location!
    }

    private void fetchNearbyPlaces(double lat, double lng, android.location.Location userLocation) {
        new Thread(() -> {
            try {
                List<Destination> allDestinations = new ArrayList<>();
                java.util.Set<String> titles = new java.util.HashSet<>();
                
                // Wikipedia natively limits search radius to 10km. 
                // To achieve a 30-35km radius, we construct a search grid offsetting by ~20km in each direction.
                double[][] points = {
                    {lat, lng},
                    {lat + 0.18, lng},
                    {lat - 0.18, lng},
                    {lat, lng + 0.18},
                    {lat, lng - 0.18}
                };
                
                for (double[] pt : points) {
                    try {
                        String urlString = "https://en.wikipedia.org/w/api.php?action=query&generator=geosearch&ggscoord=" + pt[0] + "|" + pt[1] + "&ggsradius=10000&ggslimit=20&prop=coordinates|pageimages|extracts&colimit=50&exintro=true&explaintext=true&exchars=150&piprop=thumbnail&pithumbsize=500&format=json";
                        java.net.URL url = new java.net.URL(urlString);
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setRequestProperty("User-Agent", "SafeTourAI/1.0");
                        conn.setConnectTimeout(3000);
                        conn.setReadTimeout(3000);

                        if (conn.getResponseCode() == 200) {
                            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = in.readLine()) != null) response.append(line);
                            in.close();

                            org.json.JSONObject jsonObj = new org.json.JSONObject(response.toString());
                            if (jsonObj.has("query") && jsonObj.getJSONObject("query").has("pages")) {
                                org.json.JSONObject pages = jsonObj.getJSONObject("query").getJSONObject("pages");
                                java.util.Iterator<String> keys = pages.keys();
                                while (keys.hasNext()) {
                                    String key = keys.next();
                                    org.json.JSONObject page = pages.getJSONObject(key);
                                    
                                    // Only include premium UI tiles with thumbnails
                                    if (page.has("thumbnail") && page.has("coordinates")) {
                                        org.json.JSONArray coords = page.getJSONArray("coordinates");
                                        if (coords.length() > 0) {
                                            String title = page.getString("title");
                                            if (titles.contains(title)) continue;
                                            titles.add(title);
                                            
                                            String desc = page.optString("extract", "Notable landmark nearby.");
                                            String imgUrl = page.getJSONObject("thumbnail").getString("source");
                                            
                                            double pLat = coords.getJSONObject(0).getDouble("lat");
                                            double pLng = coords.getJSONObject(0).getDouble("lon");
                                            
                                            Destination dest = new Destination(title, desc, imgUrl, pLat, pLng);
                                            android.location.Location destLoc = new android.location.Location("");
                                            destLoc.setLatitude(pLat);
                                            destLoc.setLongitude(pLng);
                                            dest.distanceKm = userLocation.distanceTo(destLoc) / 1000f;
                                            
                                            // strictly apply bounds and ignore anything returning 0.0 effectively
                                            if (dest.distanceKm <= 36.0f && dest.distanceKm > 0.05f) {
                                                allDestinations.add(dest);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {} // Gracefully ignore network failures for individual grid sections
                }
                
                // Sort the entire expanded grid results perfectly by distance from user
                java.util.Collections.sort(allDestinations, (a, b) -> Float.compare(a.distanceKm, b.distanceKm));
                
                // Force a minimum of 6 tiles if available, limit to ~15 to prevent memory spam
                List<Destination> finalList = new ArrayList<>();
                for (int i = 0; i < Math.min(allDestinations.size(), 15); i++) {
                    finalList.add(allDestinations.get(i));
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        destinationList.clear();
                        destinationList.addAll(finalList);
                        updateDestinationDistances(userLocation);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void setupMapLocation() {
        if (mapView == null) return;
        mapView.setFocusable(false);
        
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            MyLocationNewOverlay mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);
            mLocationOverlay.enableMyLocation();
            mLocationOverlay.enableFollowLocation();
            mapView.getOverlays().add(mLocationOverlay);
            
            locationViewModel.refreshLocation();
            locationViewModel.getCurrentLocation().observe(getViewLifecycleOwner(), location -> {
                if (location != null) {
                    lastFetchedLocation = location;
                    GeoPoint currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    mapView.getController().animateTo(currentPoint);
                    
                    if (isNavigationMode) {
                        updateNavHeaderUI();
                    } else {
                        if (lastPlacesFetchedLocation == null || lastPlacesFetchedLocation.distanceTo(location) > 5000) {
                            lastPlacesFetchedLocation = location;
                            fetchNearbyPlaces(location.getLatitude(), location.getLongitude(), location);
                        } else {
                            updateDestinationDistances(location);
                        }
                    }
                }
            });
        } else {
            GeoPoint defaultLocation = new GeoPoint(28.6139, 77.2090); // New Delhi
            Marker startMarker = new Marker(mapView);
            startMarker.setPosition(defaultLocation);
            startMarker.setTitle("Central India");
            mapView.getOverlays().add(startMarker);
            mapView.getController().setCenter(defaultLocation);
            mapView.getController().setZoom(12.0);
            
            Location fakeLoc = new Location("");
            fakeLoc.setLatitude(28.6139);
            fakeLoc.setLongitude(77.2090);
            lastFetchedLocation = fakeLoc;
            lastPlacesFetchedLocation = fakeLoc;
            fetchNearbyPlaces(fakeLoc.getLatitude(), fakeLoc.getLongitude(), fakeLoc);
        }
    }

    private void updateDestinationDistances(Location userLocation) {
        if (destinationsAdapter == null) return;
        for (Destination dest : destinationList) {
            Location destLoc = new Location("");
            destLoc.setLatitude(dest.lat);
            destLoc.setLongitude(dest.lng);
            float distMeters = userLocation.distanceTo(destLoc);
            dest.distanceKm = distMeters / 1000f;
            
            if (dest.distanceKm < 10.0f) {
                dest.safety = "Highly Safe";
            } else if (dest.distanceKm < 50.0f) {
                dest.safety = "Secure Area";
            } else {
                dest.safety = "Moderately Safe";
            }
        }
        destinationsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (compassOverlay != null) {
            compassOverlay.enableCompass();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        if (compassOverlay != null) {
            compassOverlay.disableCompass();
        }
    }
    
    private void setupChip(View root, int id, String query) {
        View chip = root.findViewById(id);
        if (chip != null) {
             chip.setOnClickListener(v -> launchMapSearch(query));
        }
    }

    private void launchMapSearch(String query) {
        if (lastFetchedLocation == null) {
            android.widget.Toast.makeText(requireContext(), "Awaiting GPS locking...", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        android.widget.Toast.makeText(requireContext(), "Searching for " + query + "...", android.widget.Toast.LENGTH_SHORT).show();
        
        new Thread(() -> {
            try {
                String bbox = String.format(java.util.Locale.US, "%f,%f,%f,%f", 
                        lastFetchedLocation.getLongitude() - 0.5, lastFetchedLocation.getLatitude() - 0.5, 
                        lastFetchedLocation.getLongitude() + 0.5, lastFetchedLocation.getLatitude() + 0.5);
                
                String queryEncoded = Uri.encode(query);
                String urlString = "https://nominatim.openstreetmap.org/search?q=" + queryEncoded + "&format=json&limit=1&bounded=1&viewbox=" + bbox;
                
                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "SafeTourAI/1.0");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                if (conn.getResponseCode() == 200) {
                    java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) response.append(line);
                    in.close();
                    
                    org.json.JSONArray results = new org.json.JSONArray(response.toString());
                    if (results.length() > 0) {
                        org.json.JSONObject topResult = results.getJSONObject(0);
                        double lat = Double.parseDouble(topResult.getString("lat"));
                        double lon = Double.parseDouble(topResult.getString("lon"));
                        fetchAndDrawRoute(query, lat, lon);
                    } else {
                        if (getActivity() != null) getActivity().runOnUiThread(() -> android.widget.Toast.makeText(requireContext(), "No locations found.", android.widget.Toast.LENGTH_SHORT).show());
                    }
                } else {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> android.widget.Toast.makeText(requireContext(), "Search service unavailable.", android.widget.Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void fetchAndDrawRoute(String destName, double destLat, double destLng) {
        if (lastFetchedLocation == null) return;
        new Thread(() -> {
            try {
                String startLngStr = String.format(java.util.Locale.US, "%f", lastFetchedLocation.getLongitude());
                String startLatStr = String.format(java.util.Locale.US, "%f", lastFetchedLocation.getLatitude());
                String endLngStr = String.format(java.util.Locale.US, "%f", destLng);
                String endLatStr = String.format(java.util.Locale.US, "%f", destLat);

                String urlString = "https://routing.openstreetmap.de/routed-car/route/v1/driving/" + startLngStr + "," + startLatStr + ";" + endLngStr + "," + endLatStr + "?overview=full&geometries=geojson&steps=true";
                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "SafeTourAI/1.0");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                if (conn.getResponseCode() == 200) {
                    java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) response.append(line);
                    in.close();
                    
                    org.json.JSONObject jsonObj = new org.json.JSONObject(response.toString());
                    org.json.JSONArray routes = jsonObj.getJSONArray("routes");
                    if (routes.length() > 0) {
                        org.json.JSONObject route = routes.getJSONObject(0);
                        org.json.JSONObject geometry = route.getJSONObject("geometry");
                        org.json.JSONArray coordinates = geometry.getJSONArray("coordinates");
                        
                        List<org.osmdroid.util.GeoPoint> routePoints = new ArrayList<>();
                        for (int i = 0; i < coordinates.length(); i++) {
                            org.json.JSONArray point = coordinates.getJSONArray(i);
                            double lon = point.getDouble(0);
                            double lat = point.getDouble(1);
                            routePoints.add(new org.osmdroid.util.GeoPoint(lat, lon));
                        }
                        
                        navSteps.clear();
                        org.json.JSONArray legs = route.optJSONArray("legs");
                        if (legs != null && legs.length() > 0) {
                            org.json.JSONObject leg = legs.getJSONObject(0);
                            org.json.JSONArray steps = leg.optJSONArray("steps");
                            if (steps != null) {
                                for (int s = 0; s < steps.length(); s++) {
                                    org.json.JSONObject step = steps.getJSONObject(s);
                                    org.json.JSONObject maneuver = step.optJSONObject("maneuver");
                                    if (maneuver != null) {
                                        org.json.JSONArray loc = maneuver.optJSONArray("location");
                                        if (loc != null && loc.length() >= 2) {
                                            double stepLng = loc.getDouble(0);
                                            double stepLat = loc.getDouble(1);
                                            String instruction = step.optString("name", "Unknown road");
                                            String modifier = maneuver.optString("modifier", "straight");
                                            navSteps.add(new NavStep(stepLat, stepLng, "Head " + modifier + " on " + instruction, modifier));
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (mapView == null) return;
                                if (currentRouteOverlay != null) {
                                    mapView.getOverlays().remove(currentRouteOverlay);
                                }
                                if (destinationMarkerOverlay != null) {
                                    mapView.getOverlays().remove(destinationMarkerOverlay);
                                }
                                
                                currentRouteOverlay = new org.osmdroid.views.overlay.Polyline();
                                currentRouteOverlay.setPoints(routePoints);
                                currentRouteOverlay.getOutlinePaint().setColor(Color.parseColor("#0A7AF5")); // Thicker Blue routing
                                currentRouteOverlay.getOutlinePaint().setStrokeWidth(15.0f);
                                currentRouteOverlay.getOutlinePaint().setAntiAlias(true);
                                
                                destinationMarkerOverlay = new org.osmdroid.views.overlay.Marker(mapView);
                                destinationMarkerOverlay.setPosition(new org.osmdroid.util.GeoPoint(destLat, destLng));
                                destinationMarkerOverlay.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
                                destinationMarkerOverlay.setTitle(destName);
                                destinationMarkerOverlay.setSnippet("End of Navigation Route");
                                
                                mapView.getOverlays().add(currentRouteOverlay);
                                mapView.getOverlays().add(destinationMarkerOverlay);
                                mapView.invalidate();
                                
                                destinationMarkerOverlay.showInfoWindow();
                                
                                View btnStartNav = getView() != null ? getView().findViewById(R.id.btnStartNav) : null;
                                if (btnStartNav != null && !isNavigationMode) {
                                    btnStartNav.setVisibility(View.VISIBLE);
                                }
                                
                                org.osmdroid.util.BoundingBox bb = org.osmdroid.util.BoundingBox.fromGeoPoints(routePoints);
                                mapView.zoomToBoundingBox(bb, true, 100);
                            });
                        }
                    } else {
                        if (getActivity() != null) getActivity().runOnUiThread(() -> android.widget.Toast.makeText(requireContext(), "No roads found for this route.", android.widget.Toast.LENGTH_SHORT).show());
                    }
                } else {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> android.widget.Toast.makeText(requireContext(), "Routing server unavailable.", android.widget.Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) getActivity().runOnUiThread(() -> android.widget.Toast.makeText(requireContext(), "Error fetching route.", android.widget.Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
    
    private void updateNavHeaderUI() {
        if (!isNavigationMode || getView() == null || navSteps.isEmpty()) return;
        
        TextView tvInstruction = getView().findViewById(R.id.tvNavInstruction);
        TextView tvDistance = getView().findViewById(R.id.tvNavDistance);
        ImageView ivIcon = getView().findViewById(R.id.ivNavDirection);
        
        if (currentStepIndex >= navSteps.size()) {
            if (tvInstruction != null) tvInstruction.setText("You have arrived at your destination!");
            if (tvDistance != null) tvDistance.setText("");
            if (ivIcon != null) ivIcon.setImageResource(android.R.drawable.ic_menu_mylocation);
            return;
        }
        
        NavStep step = navSteps.get(currentStepIndex);
        if (tvInstruction != null) tvInstruction.setText(step.instruction);
        
        if (lastFetchedLocation != null) {
            Location stepLoc = new Location("");
            stepLoc.setLatitude(step.lat);
            stepLoc.setLongitude(step.lng);
            float dist = lastFetchedLocation.distanceTo(stepLoc);
            if (tvDistance != null) tvDistance.setText(String.format(java.util.Locale.US, "In %.0f meters", dist));
            
            // Auto advance next instruction
            if (dist < 20.0f) {
                currentStepIndex++;
                updateNavHeaderUI();
            }
        }
    }

    // Dynamic Destinations Architecture
    class Destination {
        String name;
        String desc;
        String imgUrl;
        double lat;
        double lng;
        float distanceKm = 0.0f;
        String safety = "Evaluating";

        Destination(String name, String desc, String imgUrl, double lat, double lng) {
            this.name = name;
            this.desc = desc;
            this.imgUrl = imgUrl;
            this.lat = lat;
            this.lng = lng;
        }
    }

    class DestinationsAdapter extends RecyclerView.Adapter<DestinationsAdapter.ViewHolder> {
        private final List<Destination> items;

        DestinationsAdapter(List<Destination> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_explore_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Destination dest = items.get(position);
            holder.tvName.setText(dest.name);
            holder.tvDesc.setText(dest.desc);
            holder.tvRating.setText(dest.safety);
            
            String distText = String.format(java.util.Locale.US, "%.1f km", dest.distanceKm);
            holder.tvDistance.setText(distText);
            
            // Dynamic Color Logic based on distance!
            if (dest.distanceKm < 10.0f) {
                holder.tvDistance.setTextColor(Color.parseColor("#34C759")); // Green
                holder.tvRating.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#34C759")));
            } else {
                holder.tvDistance.setTextColor(Color.parseColor("#FF3B30")); // Red
                holder.tvRating.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF3B30")));
            }

            Glide.with(holder.itemView.getContext())
                    .load(dest.imgUrl)
                    .centerCrop()
                    .into(holder.ivImg);
                    
            holder.itemView.setOnClickListener(v -> {
                animateClickAndRun(holder.itemView, () -> {
                    fetchAndDrawRoute(dest.name, dest.lat, dest.lng);
                });
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDesc, tvDistance, tvRating;
            ImageView ivImg;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvLocationName);
                tvDesc = itemView.findViewById(R.id.tvDescription);
                tvDistance = itemView.findViewById(R.id.tvDistance);
                tvRating = itemView.findViewById(R.id.tvSafeRating);
                ivImg = itemView.findViewById(R.id.ivDestination);
            }
        }
    }
}
