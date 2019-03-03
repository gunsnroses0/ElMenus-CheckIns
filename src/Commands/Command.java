package Commands;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public abstract class Command implements Runnable {

	protected HashMap<String, Object> parameters;

	final public void init(HashMap<String, Object> parameters) {
		this.parameters = parameters;
	}

	protected abstract void execute() throws NoSuchMethodException, SecurityException, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;

	@Override
	public void run() {
		try {
			this.execute();
		} catch (NoSuchMethodException | SecurityException | ClassNotFoundException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
    public static JSONObject jsonFromMap(HashMap<String, Object> map) {
        JSONObject jsonData = new JSONObject();

        for (String key : map.keySet()) {
            Object value = map.get(key);
            if (value instanceof Map<?, ?>) {
                value = jsonFromMap((HashMap<String, Object>) value);
            }
            jsonData.put(key, value);
        }

        return jsonData;
    }
    
    public static HashMap<String, Object> jsonToMap(JSONObject json) {
        HashMap<String, Object> retMap = new HashMap<String, Object>();

        retMap = toMap(json);

        return retMap;
    }
    public static HashMap<String, Object> toMap(JSONObject object) {
        HashMap<String, Object> map = new HashMap<String, Object>();

        Iterator<String> keysItr = object.keySet().iterator();

        while(keysItr.hasNext()) {

            String key = keysItr.next();


            Object value = object.get(key);

            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    public static List<Object> toList(JSONArray array) {
        List<Object> list = new ArrayList<Object>();
        for(int i = 0; i < array.size(); i++) {
            Object value = array.get(i);
            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }
    
    public static JSONObject jsonFromArray(ArrayList<HashMap<String, Object>> array, String root) {
        JSONObject jsonData = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        Iterator<HashMap<String, Object>> arrayIter = (Iterator<HashMap<String, Object>>) array.iterator();

        while(arrayIter.hasNext()) {
            HashMap<String, Object> obj = arrayIter.next();
            jsonArray.add(jsonFromMap(obj));
        }
        jsonData.put(root, jsonArray);
        return jsonData;
    }

}
