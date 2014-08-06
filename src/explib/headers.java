package explib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


import android.util.Log;


public class headers {
	public List<function_call> functions = new ArrayList<function_call>();
	public List<String> function_names = new ArrayList<String>();
	public List<String> function_name_list = new ArrayList<String>();
	public headers(String[] fname_array){
		List<String> nms = Arrays.asList( fname_array );//
		Iterator<String> nms1 = nms.iterator();
		while(nms1.hasNext()){
			functions.add(new function_call(nms1.next()));
			}	
		Log.e("lco",function_names.indexOf("capture 2 channels")+"");
		
	}

	public function_call fetch_function(String name){
		int pos = function_names.indexOf(name);
		if(pos==-1)return null;
		else return functions.get(pos);
		
	}
	public class function_call{
		public String name=new String();
		public String func_name=new String();
		
		public List<argument> args = new ArrayList<argument>();
		
		public function_call(String cmdstring){
			Log.e("PARSING",cmdstring);
			List<String> pieces = Arrays.asList(cmdstring.split(","));
			Iterator<String> piece_iterator = pieces.iterator();
			name=piece_iterator.next();
			func_name=piece_iterator.next();
			function_name_list.add(func_name);
			
			function_names.add(name);
			//Log.e("PARSING",name);
			while(piece_iterator.hasNext()){
				String nm,type,min,max;
				if(piece_iterator.hasNext()) nm =  piece_iterator.next();	else break;
				if(piece_iterator.hasNext()) type =  piece_iterator.next();	else break;
				if(piece_iterator.hasNext()) min =  piece_iterator.next();	else break;
				if(piece_iterator.hasNext()) max =  piece_iterator.next();	else break;
				
				
				args.add(new argument(nm,type,min,max));
				}
			
			//Iterator <argument> aa = args.iterator();
			//while(aa.hasNext()){argument x=aa.next(); Log.e("Added:",x.type+":"+x.min+":"+x.max);}
			
		}
	}
	
	public class argument{
		public String name,type,min,max;
		public argument(String nm,String t,String mn,String mx){
			name=nm;
			type=t;
			min=mn;
			max=mx;
			
		}
	}


}

