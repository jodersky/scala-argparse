package configparse

trait Api
  extends configparse.core.SettingApi
  with ArgparseReaders
  with argparse.core.TypesApi
  with YamlesqueCompat
