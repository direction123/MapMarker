package direction123.mapmarker.Model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fangxiangwang on 12/7/17.
 */

@IgnoreExtraProperties
public class GasStation {
    public String userName;
    public String latitude;
    public String longitude;
    public String type;
    public Map<String, Boolean> stars = new HashMap<>();

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
}
