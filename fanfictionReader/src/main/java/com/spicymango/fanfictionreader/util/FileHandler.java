package com.spicymango.fanfictionreader.util;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.EnvironmentCompat;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.util.Log;

import com.spicymango.fanfictionreader.Settings;

public class FileHandler {

	/**
	 * Deletes a story file if it exists
	 * 
	 * @param context
	 *            The current context
	 * @param storyId
	 *            The id of the story that should be deleted
	 * @param currentPage
	 *            The page that should be deleted
	 * @return True if the file is deleted successful, false otherwise
	 */
	public static boolean deleteFile(Context context, long storyId,
			int currentPage) {
		// Declare Variables
		final FilenameFilter filter = new FileFilter(storyId, currentPage);
		final File file = findFile(filter, context);

		//Check the file exists
		if (file == null) return false;

		//Delete the file
		return file.delete();
	}

	/**
	 * Gets the path to the emulated files directory, or null if it does not
	 * exist.
	 * 
	 * @param context
	 * @return A file pointing to the emulated files directory or null
	 */
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static File getEmulatedFilesDir(Context context) {
		File sd = null;

		int currentApiVersion = android.os.Build.VERSION.SDK_INT;

		if (currentApiVersion < android.os.Build.VERSION_CODES.GINGERBREAD) {
			// Froyo and Eclair does not have emulated storage
			return null;

		} else if (!Environment.isExternalStorageRemovable()) {
			// If the primary storage is emulated and available, pick it
			sd = context.getExternalFilesDir(null);

			// If the emulated storage is not writable, return null
			String state = Environment.getExternalStorageState();
			if (!Environment.MEDIA_MOUNTED.equals(state)) {
				return null;
			}
		} else {
			return null;
		}

		// Create the folder if it does not exist
		if (sd != null && !sd.exists()) {
			sd.mkdirs();
		}

		// Log the folder path
		if (sd != null) {
			Log.v("getEmulatedFilesDir", sd.getAbsolutePath());
		}

		return sd;
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static boolean isEmulatedFilesDirWriteable() {

		int currentApiVersion = android.os.Build.VERSION.SDK_INT;

		if (currentApiVersion < android.os.Build.VERSION_CODES.GINGERBREAD) {
			// Froyo and Eclair does not have emulated storage
			return false;

		} else if (!Environment.isExternalStorageRemovable()) {
			// If the primary storage is emulated, then check it

			String state = Environment.getExternalStorageState();
			if (Environment.MEDIA_MOUNTED.equals(state)) {
				return true;
			}
		} else {
			return false;
		}

		return false;
	}

	/**
	 * Gets the path of the SD card
	 * 
	 * @param context
	 *            The current context
	 * @return A file corresponding to the external storage directory, or null
	 *         if it does not exist.
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static File getExternalFilesDir(Context context) {
		File extFiles[] = ContextCompat.getExternalFilesDirs(context, null);
		File sd = null;

		int currentApiVersion = android.os.Build.VERSION.SDK_INT;

		if (currentApiVersion >= android.os.Build.VERSION_CODES.LOLLIPOP) {
			// Use the isExternalStorageRemovable function added in Lollipop
			for (File file : extFiles) {
				if (file != null && Environment.isExternalStorageRemovable(file)) {
					sd = file;
				}
			}

		} else if (currentApiVersion >= android.os.Build.VERSION_CODES.KITKAT) {
			// getExternalFilesDirs will return all storages on KitKat. Assume
			// the second storage is the sd card
			if (Environment.isExternalStorageRemovable()) {
				sd = extFiles[0];
			} else if (extFiles.length > 2) {
				sd = extFiles[1];
			}

		} else if (currentApiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD) {
			// Use the secondary storage if the primary one is not removable
			if (Environment.isExternalStorageRemovable()) {
				sd = extFiles[0];
			} else {
				String secStore = System.getenv("SECONDARY_STORAGE");
				String packageName = context.getPackageName();
				if (secStore != null) {
					sd = new File(secStore + "/Android/data/" + packageName
							+ "/files");
				}
			}
		} else {
			// Froyo and Eclair do not have emulated storages
			sd = extFiles[0];
		}

		if (sd != null && !sd.exists()) {
			sd.mkdirs();
		}
		if (sd != null) {
			Log.v("getExternalFilesDir", sd.getAbsolutePath());
		}

		return sd;
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static File getExternalStorageDirectory(Context context) {
		File sd = null;

		int currentApiVersion = android.os.Build.VERSION.SDK_INT;

		if (currentApiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD) {
			// Use the secondary storage if the primary one is not removable
			if (Environment.isExternalStorageRemovable()) {
				sd = Environment.getExternalStorageDirectory();
			} else {
				String secStore = System.getenv("SECONDARY_STORAGE");

				if (secStore == null) {
					return null;
				}

				sd = new File(secStore);
			}
		} else {
			// Froyo and Eclair do not have emulated storages
			sd = Environment.getExternalStorageDirectory();
		}

		if (sd == null || !sd.exists()) {
			return null;
		}
		Log.v("getExternalStrDirectory", sd.getAbsolutePath());
		return sd;
	}

	public static Spanned getFile(Context context, long storyId, int currentPage) {

		// Find story
		FilenameFilter filter = new FileFilter(storyId, currentPage);
		File file = findFile(filter, context);

		// If the story is not found, return null
		if (file == null)
			return null;

		BufferedInputStream fin = null;
		Spanned result = null;

		try {
			fin = new BufferedInputStream(new FileInputStream(file));
			byte[] buffer = new byte[(int) file.length()];
			fin.read(buffer);

			if (file.getName().contains(".htm")) {
				// HTML File
				result = Html.fromHtml(new String(buffer));
			} else if (file.getName().contains(".txt")) {
				// Text File
				result = new SpannedString(new String(buffer));
			}
		} catch (IOException e) {
			Log.e(FileHandler.class.getName(), "Error loading file", e);
		} finally{
			closeStream(fin);
		}
		return result;
	}

	public static String getRawFile(Context context, long storyId,
			int currentPage) {

		// Declare Variables
		FilenameFilter filter = new FileFilter(storyId, currentPage);
		File file = findFile(filter, context);

		if (file == null) {
			return null;
		}

		BufferedInputStream fin = null;
		String result = null;

		try {
			fin = new BufferedInputStream(new FileInputStream(file));
			byte[] buffer = new byte[(int) file.length()];
			fin.read(buffer);
			fin.close();
			result = new String(buffer);

		} catch (IOException e) {
			Log.e(FileHandler.class.getName(), "Error loading raw file", e);
		} finally {
			closeStream(fin);
		}
		return result;
	}

	/**
	 * Checks whether the external storage is writable.
	 * 
	 * @return True if the external storage is available
	 */
	public static boolean isExternalStorageWritable(Context context) {

		File sd = getExternalFilesDir(context);

		if (sd == null) {
			return false;
		}

		String state = EnvironmentCompat.getStorageState(sd);
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		} else if (Environment.MEDIA_UNKNOWN.equals(state)) {
			// If the external directory exists, an SD card is present.
			// If it doesn't, try to create the folder. Success indicates that
			// the SD card is writable
			if (sd.exists() || sd.mkdirs())
				return true;
			else
				return false;
		}
		return false;
	}

	/**
	 * 
	 * @param context
	 * @param storyId
	 * @param currentPage
	 * @param html
	 * @return True if the operation succeeded, false otherwise
	 */
	public static boolean writeFile(Context context, long storyId,
			int currentPage, String html) {
		FileOutputStream fos = null;
		try {
			final File file;
			final String filename = storyId + "_" + currentPage + ".htm";

			if (Settings.shouldWriteToSD(context)) {
				// Write the story to the SD card if possible, otherwise use the
				// emulated storage
				if (isExternalStorageWritable(context)) {
					file = new File(getExternalFilesDir(context), filename);
				} else {
					file = new File(getEmulatedFilesDir(context), filename);
				}
			} else {
				// Write the story to the internal memory
				file = new File(context.getFilesDir(), filename);
			}

			// Delete any pre-existing file; necessary in case write destination
			// is different
			deleteFile(context, storyId, currentPage);

			fos = new FileOutputStream(file);
			fos.write(html.getBytes());
		} catch (IOException e) {
			return false;
		} finally {
			closeStream(fos);
		}
		return true;
	}

	/**
	 * Closes a stream, logging any exceptions that occur
	 * @param stream
	 */
	private static void closeStream(Closeable stream) {
		if (stream == null)
			return;
		try {
			stream.close();
		} catch (Exception e) {
			Log.e(FileHandler.class.getName(), "Error closing file", e);
		}

	}

	/**
	 * Finds a file that satisfies the FilenameFilter
	 * 
	 * @param filter
	 *            The filename filter to employ
	 * @param context
	 *            The current context
	 * @return The first matching file, or null
	 */
	private static File findFile(FilenameFilter filter, Context context) {
		// Declare Variables
		File dir, match[];

		// Internal Memory
		dir = context.getFilesDir();
		match = dir.listFiles(filter);

		// Emulated Memory
		if (match == null || match.length == 0) {
			dir = getEmulatedFilesDir(context);
			if (dir != null) {
				match = dir.listFiles(filter);
			}
		}

		// External Memory
		if ((match == null || match.length == 0) && isExternalStorageWritable(context)) {
			dir = getExternalFilesDir(context);
			match = dir.listFiles(filter);
		}

		// Not found
		if (match == null || match.length == 0) {
			return null;
		}

		return match[0];
	}

	private static final class FileFilter implements FilenameFilter {
		private final Matcher matcher;

		public FileFilter(long storyId, int currentPage) {
			Pattern pattern = Pattern.compile(Long.toString(storyId) + "_"
					+ Integer.toString(currentPage) + "\\..*");
			matcher = pattern.matcher("");
		}

		@Override
		public boolean accept(File dir, String filename) {
			matcher.reset(filename);
			return matcher.matches();
		}
	}
}