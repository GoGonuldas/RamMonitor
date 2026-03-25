package com.rammonitor.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Float;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class RamHistoryDao_Impl implements RamHistoryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<RamHistoryEntry> __insertionAdapterOfRamHistoryEntry;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOlderThan;

  public RamHistoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfRamHistoryEntry = new EntityInsertionAdapter<RamHistoryEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `ram_history` (`id`,`totalRam`,`usedRam`,`usagePercent`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RamHistoryEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTotalRam());
        statement.bindLong(3, entity.getUsedRam());
        statement.bindDouble(4, entity.getUsagePercent());
        statement.bindLong(5, entity.getTimestamp());
      }
    };
    this.__preparedStmtOfDeleteOlderThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM ram_history WHERE timestamp < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final RamHistoryEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfRamHistoryEntry.insert(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOlderThan(final long before, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOlderThan.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, before);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteOlderThan.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<RamHistoryEntry>> getLast100() {
    final String _sql = "SELECT * FROM ram_history ORDER BY timestamp DESC LIMIT 100";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"ram_history"}, new Callable<List<RamHistoryEntry>>() {
      @Override
      @NonNull
      public List<RamHistoryEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTotalRam = CursorUtil.getColumnIndexOrThrow(_cursor, "totalRam");
          final int _cursorIndexOfUsedRam = CursorUtil.getColumnIndexOrThrow(_cursor, "usedRam");
          final int _cursorIndexOfUsagePercent = CursorUtil.getColumnIndexOrThrow(_cursor, "usagePercent");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<RamHistoryEntry> _result = new ArrayList<RamHistoryEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RamHistoryEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTotalRam;
            _tmpTotalRam = _cursor.getLong(_cursorIndexOfTotalRam);
            final long _tmpUsedRam;
            _tmpUsedRam = _cursor.getLong(_cursorIndexOfUsedRam);
            final float _tmpUsagePercent;
            _tmpUsagePercent = _cursor.getFloat(_cursorIndexOfUsagePercent);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new RamHistoryEntry(_tmpId,_tmpTotalRam,_tmpUsedRam,_tmpUsagePercent,_tmpTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getSince(final long since,
      final Continuation<? super List<RamHistoryEntry>> $completion) {
    final String _sql = "SELECT * FROM ram_history WHERE timestamp > ? ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, since);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<RamHistoryEntry>>() {
      @Override
      @NonNull
      public List<RamHistoryEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTotalRam = CursorUtil.getColumnIndexOrThrow(_cursor, "totalRam");
          final int _cursorIndexOfUsedRam = CursorUtil.getColumnIndexOrThrow(_cursor, "usedRam");
          final int _cursorIndexOfUsagePercent = CursorUtil.getColumnIndexOrThrow(_cursor, "usagePercent");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<RamHistoryEntry> _result = new ArrayList<RamHistoryEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RamHistoryEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTotalRam;
            _tmpTotalRam = _cursor.getLong(_cursorIndexOfTotalRam);
            final long _tmpUsedRam;
            _tmpUsedRam = _cursor.getLong(_cursorIndexOfUsedRam);
            final float _tmpUsagePercent;
            _tmpUsagePercent = _cursor.getFloat(_cursorIndexOfUsagePercent);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new RamHistoryEntry(_tmpId,_tmpTotalRam,_tmpUsedRam,_tmpUsagePercent,_tmpTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAverageUsage(final long since, final Continuation<? super Float> $completion) {
    final String _sql = "SELECT AVG(usagePercent) FROM ram_history WHERE timestamp > ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, since);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Float>() {
      @Override
      @Nullable
      public Float call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Float _result;
          if (_cursor.moveToFirst()) {
            final Float _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getFloat(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getPeakUsage(final long since, final Continuation<? super Float> $completion) {
    final String _sql = "SELECT MAX(usagePercent) FROM ram_history WHERE timestamp > ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, since);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Float>() {
      @Override
      @Nullable
      public Float call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Float _result;
          if (_cursor.moveToFirst()) {
            final Float _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getFloat(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
