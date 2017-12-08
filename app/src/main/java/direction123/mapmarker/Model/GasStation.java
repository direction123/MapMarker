package direction123.mapmarker.Model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fangxiangwang on 12/7/17.
 */

@IgnoreExtraProperties
public class GasStation implements Parcelable {
    public String userName;
    public String latitude;
    public String longitude;
    public String type;

    public GasStation() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
    }

    public GasStation(String userName, String latitude, String longitude, String type) {
        this.userName = userName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("userName", userName);
        result.put("latitude", latitude);
        result.put("longitude", longitude);
        result.put("type", type);

        return result;
    }

    protected GasStation(Parcel in) {
        userName = in.readString();
        latitude = in.readString();
        longitude = in.readString();
        type = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userName);
        dest.writeString(latitude);
        dest.writeString(longitude);
        dest.writeString(type);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<GasStation> CREATOR = new Parcelable.Creator<GasStation>() {
        @Override
        public GasStation createFromParcel(Parcel in) {
            return new GasStation(in);
        }

        @Override
        public GasStation[] newArray(int size) {
            return new GasStation[size];
        }
    };
}
