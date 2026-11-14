package com.absinthe.libchecker.domain.snapshot.backup.ui

import com.absinthe.libchecker.database.backup.RoomBackup

/** Host of the snapshot backup sheet that provides the Room database backup helper. */
interface SnapshotRoomBackupOwner {
  val snapshotRoomBackup: RoomBackup
}
