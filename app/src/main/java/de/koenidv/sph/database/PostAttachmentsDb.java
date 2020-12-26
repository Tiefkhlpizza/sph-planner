package de.koenidv.sph.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.koenidv.sph.objects.PostAttachment;

public class PostAttachmentsDb {

    private final DatabaseHelper dbhelper = DatabaseHelper.getInstance();

    private static PostAttachmentsDb instance;

    private PostAttachmentsDb() {
    }

    public static PostAttachmentsDb getInstance() {
        if (PostAttachmentsDb.instance == null) {
            PostAttachmentsDb.instance = new PostAttachmentsDb();
        }
        return PostAttachmentsDb.instance;
    }

    public void save(List<PostAttachment> postAttachments) {
        for (PostAttachment postAttachment : postAttachments) {
            save(postAttachment);
        }
    }


    public void save(PostAttachment postAttachment) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("attachment_id", postAttachment.getAttachmentId());
        cv.put("id_course", postAttachment.getId_course());
        cv.put("id_post", postAttachment.getId_post());
        cv.put("name", postAttachment.getName());
        cv.put("date", postAttachment.getDate().getTime() / 1000);
        cv.put("url", postAttachment.getUrl());
        cv.put("size", postAttachment.getFileSize());
        cv.put("type", postAttachment.getFileType());

        Cursor cursor = db.rawQuery("SELECT * FROM postAttachments WHERE attachment_id = '" + postAttachment.getAttachmentId() + "'", null);
        if (cursor.getCount() == 0) {
            db.insert("postAttachments", null, cv);
        } else {
            db.update("postAttachments", cv, "attachment_id = '" + postAttachment.getAttachmentId() + "'", null);
        }
        cursor.close();
    }

    public List<PostAttachment> getPostByCourseId(String course_id) {
        List<PostAttachment> returnList = new ArrayList<>();
        final SQLiteDatabase db = dbhelper.getReadableDatabase();

        String queryString = "SELECT * FROM postAttachments WHERE id_course = '" + course_id + "'";

        Cursor cursor = db.rawQuery(queryString, null);
        if (cursor.moveToFirst()) {
            do {
                String attachmentId = cursor.getString(0);
                String id_course = cursor.getString(1);
                String id_post = cursor.getString(2);
                String name = cursor.getString(3);
                Date date = new Date(cursor.getInt(4) * 1000L);
                String url = cursor.getString(5);
                String size = cursor.getString(6);
                String type = cursor.getString(7);

                PostAttachment newPostAttachment = new PostAttachment(attachmentId, id_course, id_post, name, date, url, size, type);

                returnList.add(newPostAttachment);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return returnList;
    }

    public List<PostAttachment> getPostByPostId(String postid) {
        List<PostAttachment> returnList = new ArrayList<>();
        final SQLiteDatabase db = dbhelper.getReadableDatabase();

        String queryString = "SELECT * FROM postAttachments WHERE id_post = '" + postid + "'";

        Cursor cursor = db.rawQuery(queryString, null);
        if (cursor.moveToFirst()) {
            do {
                String attachmentId = cursor.getString(0);
                String id_course = cursor.getString(1);
                String id_post = cursor.getString(2);
                String name = cursor.getString(3);
                Date date = new Date(cursor.getInt(4) * 1000L);
                String url = cursor.getString(5);
                String size = cursor.getString(6);
                String type = cursor.getString(7);

                PostAttachment newPostAttachment = new PostAttachment(attachmentId, id_course, id_post, name, date, url, size, type);

                returnList.add(newPostAttachment);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return returnList;
    }
}
