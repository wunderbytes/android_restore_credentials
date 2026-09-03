
import 'android_restore_credentials_platform_interface.dart';

class AndroidRestoreCredentials {
  Future<String?> getPlatformVersion() {
    return AndroidRestoreCredentialsPlatform.instance.getPlatformVersion();
  }
}
