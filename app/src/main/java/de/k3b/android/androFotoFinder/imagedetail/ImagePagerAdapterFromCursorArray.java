package de.k3b.android.androFotoFinder.imagedetail;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.util.ArrayList;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.util.MediaScanner;
import de.k3b.database.SelectedItems;
import uk.co.senab.photoview.PhotoView;

/**
 * Purpose: allow viewing images from ".nomedia" folders where no data is available in mediadb/cursor.
 * Same as ImagePagerAdapterFromCursor but while underlaying cursor has
 * no data photos are taken from array instead.
 *
 * Created by k3b on 12.04.2016.
 */
public class ImagePagerAdapterFromCursorArray extends ImagePagerAdapterFromCursor {

    /**
     * Implements the array sepecific stuff that hopefully can be reused in other adapters, too
     */
    public class AdapterArrayHelper {
        /** not null data comes from array instead from base implementation */
        private String[] mFullPhotoPaths = null;

        public AdapterArrayHelper(Activity context, String fullPhotoPaths) {
            File parentDir = MediaScanner.getDir(fullPhotoPaths);
            mFullPhotoPaths = parentDir.list(MediaScanner.JPG_FILENAME_FILTER);
            if ((mFullPhotoPaths != null) && (mFullPhotoPaths.length == 0)) {
                mFullPhotoPaths = null;
            } else {
                // #33
                // convert to absolute paths
                String parentDirString = parentDir.getAbsolutePath();
                for (int i = 0; i < mFullPhotoPaths.length; i++) {
                    mFullPhotoPaths[i] = parentDirString + "/" + mFullPhotoPaths[i];
                }

                if (Global.mustRemoveNOMEDIAfromDB) {
                    FotoSql.execDeleteByPath(context, parentDirString);
                }
            }
        }

        public int getCount() {
            return mFullPhotoPaths.length;
        }

        /** return null if no file array or illegal index */
        private String getFullFilePathfromArray(int position) {
            if ((mFullPhotoPaths != null) && (position >= 0) && (position < mFullPhotoPaths.length)) {
                return mFullPhotoPaths[position];
            }
            return null;
        }

        /** internal helper. return -1 if position is not available */
        public int getPositionFromPath(String path) {
            if ((mFullPhotoPaths != null) && (path != null)) {
                int result = -1;
                for (int position = 0; position < mFullPhotoPaths.length; position++) {
                    if (path.equalsIgnoreCase(mFullPhotoPaths[position])) {
                        result = position;
                        break;
                    }
                }
                if (Global.debugEnabledViewItem) Log.i(Global.LOG_CONTEXT, mDebugPrefix + "getPositionFromPath-Array(" + path +") => " + result);
                return result;
            }
            return -1;
        }

        /** helper for SelectedItems.Id2FileNameConverter: converts items.id-s to string array of filenNames. */
        public String[] getFileNames(SelectedItems items) {
            if (items != null) {
                ArrayList<String> result = new ArrayList<>();

                int size = 0;
                for(Long id : items) {
                    String path = (id != null) ? getFullFilePathfromArray(convertBetweenPositionAndId(id.intValue())) : null;
                    result.add(path);
                    if (path != null) size++;
                }

                if (size > 0) {
                    return result.toArray(new String[size]);
                }
            }
            return null;
        }

        /** translates offset in adapter to id of image */
        public long getImageId(int position) {
            return convertBetweenPositionAndId(position);
        }

        /** translates offset in adapter to id of image and vice versa */
        private int convertBetweenPositionAndId(int value) {
            return -2 -value;
        }
    }

    /** not null data comes from array instead from base implementation */
    private AdapterArrayHelper mArrayImpl = null;

    public ImagePagerAdapterFromCursorArray(final Activity context, String name, String fullPhotoPaths) {
        super(context, name);

        if (MediaScanner.isNoMedia(fullPhotoPaths,22)) {
            mArrayImpl = new AdapterArrayHelper(context, fullPhotoPaths);
        }
    }

    /** get informed that cursordata may be available so array can be disabled */
    @Override
    public Cursor swapCursor(Cursor newCursor) {
        Cursor oldCursor = super.swapCursor(newCursor);
        if (super.getCount() > 0) {
            // cursor has data => disable aray
            this.mArrayImpl = null;
        }
        return oldCursor;
    }

    @Override
    public int getCount() {
        if (mArrayImpl != null) {
            return mArrayImpl.getCount();
        }

        return super.getCount();
    }

    @Override
    public String getFullFilePath(int position) {
        if (mArrayImpl != null) return mArrayImpl.getFullFilePathfromArray(position);
        return super.getFullFilePath(position);
    }

    /** translates offset in adapter to id of image */
    @Override
    public long getImageId(int position) {
        if (mArrayImpl != null) return mArrayImpl.getImageId(position);
        return super.getImageId(position);
    }

    @Override
    public View instantiateItem(ViewGroup container, int position) {
        final String fullPhotoPathFromArray = (mArrayImpl != null) ? mArrayImpl.getFullFilePathfromArray(position) : null;
        if (fullPhotoPathFromArray != null) {
            // special case image from ".nomedia" folder via absolute path not via content: uri

            PhotoView photoView = new PhotoView(container.getContext());
            photoView.setMaximumScale(20);
            photoView.setMediumScale(5);

            if (Global.debugEnabledViewItem) Log.i(Global.LOG_CONTEXT, mDebugPrefix + "instantiateItemFromArray(#" + position +") => " + fullPhotoPathFromArray + " => " + photoView);

            photoView.setImageURI(Uri.parse(fullPhotoPathFromArray));

            container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            return photoView;

        }

        // no array avaliable. Use original cursor baed implementation
        return  super.instantiateItem(container,position);
    }

    /** internal helper. return -1 if position is not available */
    @Override
    public int getPositionFromPath(String path) {
        if (mArrayImpl != null) {
            int result = mArrayImpl.getPositionFromPath(path);

            if (Global.debugEnabledViewItem) Log.i(Global.LOG_CONTEXT, mDebugPrefix + "getPositionFromPath-Array(" + path +") => " + result);
            return result;
        }
        return super.getPositionFromPath(path);
    }


    /** SelectedItems.Id2FileNameConverter: converts items.id-s to string array of filenNames via media database. */
    @Override
    public String[] getFileNames(SelectedItems items) {
        if (mArrayImpl != null) return mArrayImpl.getFileNames(items);
        return super.getFileNames(items);
    }
}
