package configparse

trait Api
  extends configparse.core.SettingApi
  with ArgparseReaders
  with ArgparseParams
  with argparse.core.TypesApi
  with YamlesqueCompat
