package com.example.apoc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.apoc.DB.DBItem;
import com.example.apoc.DB.DBWrapper;
import com.example.apoc.DB.GroupsDB;
import com.example.apoc.DB.ItemsDB;
import com.example.apoc.DB.LogDB;
import com.example.apoc.DB.UsersDB;
import com.example.apoc.Storage.ImagesDB;
import com.example.apoc.location.LocationInfo;
import com.example.apoc.location.LocationTracker;
import com.example.apoc.types.Fears;
import com.example.apoc.types.GridDisplay;
import com.example.apoc.types.Group;
import com.example.apoc.types.Item;
import com.example.apoc.types.ItemCount;
import com.example.apoc.types.Log;
import com.example.apoc.types.Message;
import com.example.apoc.types.User;
import com.example.apoc.types.UserStatus;
//import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;


public class ProfileEdit extends AppCompatActivity {

    public static final String USER_DATA = "user";
    public final String STATUS_UNCHANGED = "You must choose your status to proceed!";
    public final String LOCATION_UNDEFINED = "You must set your location to proceed!";
    public final String PROFILE_UPDATED_LOG = "%s info has been updated";
    private static final int PICK_IMAGE_REQUEST = 1;
    private final int LOCATION_ACCURACY = 20;

    private ImagesDB imagesDB;

    private ImageView image;
    private Button pickImg;
    private TextView email;
    private EditText nickname;
    private EditText phone;
    private Button location;
    private SwitchCompat status;
    private SwitchCompat statusInGroup;
    private GridLayout gridLayouts;

    private Button save;

    private LinearLayout statusInGroupLayout;
    private LocationInfo newLocation;
    private Uri imageUri;

    private User user;
    private Context cnt;
    LocationTracker locationTracker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);
        cnt = this;
        imagesDB = new ImagesDB();

        Intent intent = getIntent();
        user = (User) intent.getSerializableExtra(USER_DATA);
        if (user == null) {
            finish();
        }
        initializeUI();

    }

    private void initializeUI() {
        gridLayouts = findViewById(R.id.profile_edit_grid);
        GridDisplay gridDisplay = new GridDisplay(getApplicationContext(),user, gridLayouts, true, 4);

        image = findViewById(R.id.image);

        if(!user.getImageUrl().equals("")){
            ImagesDB.showCircleImage(user.getImageUrl(),image,this);
        }
        pickImg = findViewById(R.id.pickImg);

        pickImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        email = findViewById(R.id.email);
        email.setText(user.getEmail());

        nickname = findViewById(R.id.nickname);
        nickname.setText(user.getNickName());

        phone = findViewById(R.id.phone);
        phone.setText(user.getPhone());

        location = findViewById(R.id.location);
        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // add waiting indication
                locationTracker = new LocationTracker(cnt);

                locationTracker.setLocationUpdateListener(new LocationTracker.OnLocationUpdateListener() {
                    @Override
                    public void onLocationUpdate() {
                        if(locationTracker.getInfo().getAccuracy() <= LocationTracker.ACCURACY){
                            newLocation = locationTracker.getInfo();
                            locationTracker.stopTracking();
                            Toast.makeText(cnt,"Location Updated",Toast.LENGTH_LONG).show();
                        }
                    }
                });
//                if(location.startTracking()) {
//                    newLocation = location.getInfo();
//                }
//                else{
//                    // todo notify location didnt work
//                }
                locationTracker.startTracking();
            }
        });

        status = findViewById(R.id.status);
        statusInGroup = findViewById(R.id.status_in_group);
        statusInGroupLayout = findViewById(R.id.status_layout);

//        statusInGroupLayout.setVisibility(View.INVISIBLE);
        if(!user.isUndefined()){
            status.setChecked(!user.isLoneWolf());
            statusInGroup.setChecked(user.isAlpha());

            status.setEnabled(false);
            statusInGroup.setEnabled(false);
        }

        status.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
//                    statusInGroupLayout.setVisibility(View.VISIBLE);
                    statusInGroup.setEnabled(true);
                }
                else{
//                    statusInGroupLayout.setVisibility(View.INVISIBLE);
                    statusInGroup.setEnabled(false);
                }
            }
        });

        save = findViewById(R.id.save);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });
    }

    private void saveProfile(){

        UsersDB udb = new UsersDB();
        final GroupsDB groupsDB = new GroupsDB();

            if (!status.isChecked()) {
                user.setStatus(UserStatus.loneWolf.name());
            } else {
                if (statusInGroup.isChecked()) {
                    user.setStatus(UserStatus.alpha.name());

                    groupsDB.getGroupByUser(user.getId());
                    groupsDB.setDataChangeListener(new DBWrapper.OnDataChangeListener() {
                        @Override
                        public void onGetAll() {

                        }

                        @Override
                        public void onGetSpecific() {
                            Group group = (Group)groupsDB.getItemById(user.getId());
                            if(group == null){
                                group = new Group(user.getNickName(), user.getId(), user.getFears());
                                groupsDB.addItem(group);
                                group.addMember(user);
                            }
                            else {
                                group.setFears(user.getFears());
                                groupsDB.updateItem(group);
                            }
                        }
                    });
                } else {
                    user.setStatus(UserStatus.beta.name());
                }
            }


        user.setNickName(nickname.getText().toString());
        user.setPhone(phone.getText().toString());
        if(newLocation!=null) {
            user.setLocationInfo(newLocation);
        }

        if(user.getLocationInfo() == null){
            Toast.makeText(cnt,LOCATION_UNDEFINED,Toast.LENGTH_LONG).show();
            return;
        }
        if(user.isUndefined())
        {
            Toast.makeText(cnt,STATUS_UNCHANGED,Toast.LENGTH_LONG).show();
            return;
        }

        imagesDB.Upload(imageUri, user, this);
        updateItems(udb); // at end updates users db
        Toast.makeText(cnt,"Profile updated",Toast.LENGTH_LONG).show();

    }

    private void updateItems(final UsersDB udb){
        final ItemsDB itemsDB = new ItemsDB();
        itemsDB.getItemsByFears(user.getFears());
        itemsDB.setDataChangeListener(new DBWrapper.OnDataChangeListener() {
            @Override
            public void onGetAll() {
            }


            @Override
            public void onGetSpecific() {
                ArrayList<ItemCount> newItems = new ArrayList<>();

                for(DBItem item : itemsDB.getItems().values()){
                    boolean added = false;
                    for(ItemCount itemCount : user.getItems()){
                        if(item.getId().equals(itemCount.getName())){
                            itemCount.setMax(getMaxCountByFears((Item)item, user.getFears()));
                            newItems.add(itemCount);
                            added = true;
                            break;
                        }
                    }
                    if(!added) {
                        newItems.add(new ItemCount(item.getId()));
                    }

                }
                user.setItems(newItems);
                udb.updateItem(user);
                (new LogDB()).addItem(new Message(String.format(PROFILE_UPDATED_LOG,user.getId()),user.getId()));
                finish();
            }
        });
    }

    private double getMaxCountByFears(Item item, ArrayList<Fears> fears){
        double max = 0;
        for(Fears fear : fears){
            if(item.getAmount(fear) > max){
                max = item.getAmount(fear);
            }
        }
        return max;
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri ourImageUri = data.getData();
            imageUri = ourImageUri;

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver() , ourImageUri);
                ImagesDB.showCircleBitmapImage(bitmap,image,this);
//                image.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }

//            Picasso.with(this).load(ourImageUri).into(image);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permission, @NonNull int[] grantResults) {
        locationTracker.onPermissionResult(requestCode, permission, grantResults);
    }
}