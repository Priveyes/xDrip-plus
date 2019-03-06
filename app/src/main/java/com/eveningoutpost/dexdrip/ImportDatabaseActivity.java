package com.eveningoutpost.dexdrip;

import android.*;
import android.content.DialogInterface.*;
import android.content.*;
import android.content.pm.*;
import android.database.sqlite.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.*;

import androidx.annotation.*;
import androidx.appcompat.app.*;
import androidx.core.app.*;
import androidx.core.content.*;

import com.eveningoutpost.dexdrip.models.*;
import com.eveningoutpost.dexdrip.utilitymodels.*;
import com.eveningoutpost.dexdrip.utils.*;
import com.eveningoutpost.dexdrip.wearintegration.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import static com.eveningoutpost.dexdrip.Home.*;

public class ImportDatabaseActivity extends ListActivityWithMenu {
	private final static String TAG = ImportDatabaseActivity.class.getSimpleName();
	public static String menu_name = "Import Database";
	private Handler mHandler;
	private ArrayList<String> databaseNames;
	private ArrayList<File> databases;
	private final static int MY_PERMISSIONS_REQUEST_STORAGE = 132;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.OldAppTheme); // or null actionbar
		super.onCreate(savedInstanceState);
		mHandler = new Handler();
		setContentView(R.layout.activity_import_db);
		final String importit = getIntent().getStringExtra("importit");
		if ((importit != null) && (!importit.isEmpty())) {
			importDB(new File(importit), this);
		} else {
			showWarningAndInstructions();
		}
	}

	private void generateDBGui() {
		int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
		if (permissionCheck == PackageManager.PERMISSION_GRANTED && findAllDatabases()) {
			sortDatabasesAlphabetically();
			showDatabasesInList();
		} else if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
			JoH.static_toast_long("Need permission for saved files");
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_STORAGE);
		} else {
			postImportDB("\'xdrip\' is not a directory... aborting.");
		}
	}

	private void showWarningAndInstructions() {
		LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(R.layout.import_db_warning, null);
		androidx.appcompat.app.AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setTitle("Import Instructions");
		alertDialog.setView(view);
		alertDialog.setCancelable(false);
		alertDialog.setPositiveButton(R.string.ok, (OnClickListener) (dialog, which) -> generateDBGui());
		AlertDialog alert = alertDialog.create();
		alert.show();
	}

	private void sortDatabasesAlphabetically() {
		Collections.sort(databases, (lhs, rhs) -> {
			//descending sort
			return rhs.getName().compareTo(lhs.getName());
		});
	}

	private boolean findAllDatabases() {
		databases = new ArrayList<>();

		File file = new File(FileUtils.getExternalDir());
		if (!FileUtils.makeSureDirectoryExists(file.getAbsolutePath())) {
			return false;
		}

		// add from "root"
		addAllDatabases(file, databases);

		// add from level below (Andriod usually unzips to a subdirectory)
		File[] subdirectories = file.listFiles(File::isDirectory);
		try {
			for (File subdirectory : subdirectories) {
				addAllDatabases(subdirectory, databases);
			}
		} catch (NullPointerException e) {
			// nothing found
		}
		return true;
	}

	private void showDatabasesInList() {
		databaseNames = new ArrayList<>();

		//show found databases in List
		for (File db : databases) {
			databaseNames.add(db.getName());
		}

		//PP
		ListView listViewDataBase = (ListView) findViewById(R.id.listViewDB);
		final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, databaseNames);
		//setListAdapter(adapter);

		//PP
		listViewDataBase.setOnItemClickListener((parent, view, position, id) -> {
			AlertDialog.Builder builder = new AlertDialog.Builder(ImportDatabaseActivity.this);
			builder.setPositiveButton(R.string.ok, (OnClickListener) (dialog, id12) -> importDB(position));
			builder.setNegativeButton(R.string.cancel, (OnClickListener) (dialog, id1) -> {
				//do nothing
			});
			builder.setTitle("Confirm Import");
			builder.setMessage("Do you really want to import '" + databases.get(position).getName() + "'?\n This may negatively affect the data integrity of your system!");
			AlertDialog dialog = builder.create();
			dialog.show();
		});

		//PP
		listViewDataBase.setAdapter(adapter);

		if (databaseNames.isEmpty()) {
			postImportDB("No databases found.");
		}
	}

	private void addAllDatabases(File file, ArrayList<File> databases) {
		File[] files = file.listFiles(pathname -> pathname.getPath().endsWith(".sqlite") || pathname.getPath().endsWith(".zip"));
		if ((databases != null) && (files != null)) {
			Collections.addAll(databases, files);
		}
	}
	/*FIXME*/
//    @Override
//    protected void onListItemClick(ListView l, View v, final int position, long id) {
//
//        androidx.appcompat.app.AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setPositiveButton(R.string.ok, (OnClickListener) (dialog, id12) -> importDB(position));
//        builder.setNegativeButton(R.string.cancel, (OnClickListener) (dialog, id1) -> {
//            //do nothing
//        });
//        builder.setTitle("Confirm Import");
//        builder.setMessage("Do you really want to import '" + databases.get(position).getName() + "'?\n This may negatively affect the data integrity of your system!");
//        AlertDialog dialog = builder.create();
//        dialog.show();
//    }

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	public String getMenuName() {
		return menu_name;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == MY_PERMISSIONS_REQUEST_STORAGE) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				generateDBGui();
			} else {
				finish();
			}
		}
	}

	public int getDBVersion() {

		int version = -1;
		try {
			ApplicationInfo ai = getPackageManager().getApplicationInfo(this.getPackageName(), PackageManager.GET_META_DATA);
			Bundle bundle = ai.metaData;
			version = bundle.getInt("AA_DB_VERSION");
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return version;
	}

	private void importDB(int position) {
		importDB(databases.get(position), this);
	}

	private void importDB(File the_file, AppCompatActivity activity) {
		androidx.appcompat.app.AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle("Importing, please wait");
		builder.setMessage("Importing, please wait");
		AlertDialog dialog = builder.create();
		dialog.show();
		dialog.setMessage("Step 1: checking prerequisites");
		dialog.setCancelable(false);
		LoadTask lt = new LoadTask(dialog, the_file);
		lt.execute();
	}

	protected void postImportDB(String result) {

		startWatchUpdaterService(this, WatchUpdaterService.ACTION_RESET_DB, TAG);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setPositiveButton(R.string.ok, (OnClickListener) (dialog, id) -> returnToHome());
		builder.setTitle("Import Result");
		builder.setMessage(result);
		AlertDialog dialog = builder.create();
		dialog.show();

	}

	private void returnToHome() {
		Intent intent = new Intent(this, Home.class);
		CollectionServiceStarter.restartCollectionService(getApplicationContext());
		startActivity(intent);
		finish();
	}

	private class LoadTask extends AsyncTask<Void, Void, String> {

		private final AlertDialog statusDialog;
		private File dbFile;

		LoadTask(AlertDialog statusDialog, File dbFile) {
			super();
			this.statusDialog = statusDialog;
			this.dbFile = dbFile;
		}

		protected String doInBackground(Void... args) {
			//Check if db has the correct version:
			File delete_file = null;
			try {

				if (dbFile.getAbsolutePath().endsWith(".zip")) {
					// uncompress first
					try {
						final FileInputStream fileInputStream = new FileInputStream(dbFile.getAbsolutePath());
						final ZipInputStream zip_stream = new ZipInputStream(new BufferedInputStream(fileInputStream));
						ZipEntry zipEntry = zip_stream.getNextEntry();
						if ((zipEntry != null) && zipEntry.isDirectory()) {
							zipEntry = zip_stream.getNextEntry();
						}
						if (zipEntry != null) {
							String filename = zipEntry.getName();
							if (filename.endsWith(".sqlite")) {
								String output_filename = dbFile.getAbsolutePath().replaceFirst(".zip$", ".sqlite");
								FileOutputStream fout = new FileOutputStream(output_filename);
								byte[] buffer = new byte[4096];
								int count = 0;
								while ((count = zip_stream.read(buffer)) != -1) {
									fout.write(buffer, 0, count);
								}
								fout.close();
								dbFile = new File(output_filename);
								delete_file = dbFile;
								Log.d(TAG, "New filename: " + output_filename);
							} else {
								String msg = "Cant find sqlite in zip file";
								JoH.static_toast_long(msg);
								return msg;
							}
							zip_stream.closeEntry();
						} else {
							String msg = "Invalid ZIP file";
							JoH.static_toast_long(msg);
							return msg;
						}

						zip_stream.close();
						fileInputStream.close();

					} catch (IOException e) {
						String msg = "Could not open file";
						JoH.static_toast_long(msg);
						return msg;
					}
				}

				SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
				int version = db.getVersion();
				db.close();
				if (getDBVersion() != version) {
					statusDialog.dismiss();
					return "Wrong Database version.\n(" + version + " instead of " + getDBVersion() + ")";
				}
			} catch (SQLiteException e) {
				statusDialog.dismiss();
				return "Database cannot be opened... aborting.";
			}
			mHandler.post(() -> statusDialog.setMessage("Step 2: exporting current DB"));

			String export = DatabaseUtil.saveSql(xdrip.getAppContext(), "b4import");

			if (export == null) {
				statusDialog.dismiss();
				return "Exporting database not successfull... aborting.";
			}

			mHandler.post(() -> statusDialog.setMessage("Step 3: importing DB"));

			String result = DatabaseUtil.loadSql(xdrip.getAppContext(), dbFile.getAbsolutePath());
			if (delete_file != null) delete_file.delete();
			statusDialog.dismiss();
			return result;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			postImportDB(result);

		}
	}
}
