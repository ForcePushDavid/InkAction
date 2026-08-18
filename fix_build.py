import os

manifest_path = r'c:\Users\David\Desktop\ai tužka\app\src\main\AndroidManifest.xml'
with open(manifest_path, 'r', encoding='utf-8') as f:
    manifest = f.read()

provider_xml = '''
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities=".fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
    </application>'''

if 'FileProvider' not in manifest:
    manifest = manifest.replace('</application>', provider_xml)
    with open(manifest_path, 'w', encoding='utf-8') as f:
        f.write(manifest)

xml_dir = r'c:\Users\David\Desktop\ai tužka\app\src\main\res\xml'
if not os.path.exists(xml_dir):
    os.makedirs(xml_dir)

file_paths_xml = '''<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="cache" path="." />
</paths>'''
with open(os.path.join(xml_dir, 'file_paths.xml'), 'w', encoding='utf-8') as f:
    f.write(file_paths_xml)

mws_path = r'c:\Users\David\Desktop\ai tužka\app\src\main\java\com\inkaction\app\ui\screens\MainWorkspaceScreen.kt'
with open(mws_path, 'r', encoding='utf-8') as f:
    mws = f.read()

if 'val backupImportLauncher' not in mws:
    replacement1 = '''var showSettings by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val backupImportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val success = com.inkaction.app.util.BackupUtil.importFromZip(context, uri, context.filesDir)
            if (success) {
                android.widget.Toast.makeText(context, "Záloha obnovena. Restartujte aplikaci.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }'''
    mws = mws.replace('var showSettings by remember { mutableStateOf(false) }', replacement1)

if 'onExportBackup =' not in mws:
    replacement2 = '''onDismiss = { showSettings = false },
            onExportBackup = {
                val uri = com.inkaction.app.util.BackupUtil.exportToZip(context, context.filesDir)
                if (uri != null) {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Sdílet zálohu"))
                }
            },
            onImportBackup = {
                backupImportLauncher.launch("application/zip")
            },'''
    mws = mws.replace('onDismiss = { showSettings = false },', replacement2)

with open(mws_path, 'w', encoding='utf-8') as f:
    f.write(mws)