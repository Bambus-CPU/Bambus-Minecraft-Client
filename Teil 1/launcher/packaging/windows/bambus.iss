; Bambus Launcher – Windows-Installationsprogramm (Inno Setup 6)
; Wird automatisch auf GitHub gebaut (siehe .github/workflows/build.yml).
; Installiert ohne Administratorrechte nach %LOCALAPPDATA%\Programs\Bambus Launcher,
; legt Startmenü- und Desktop-Verknüpfung an und startet den Launcher danach.

#define AppName "Bambus Launcher"
#define AppExe "Bambus Launcher.exe"
#ifndef AppVersion
  #define AppVersion "3.3.0"
#endif
#ifndef SourceDir
  #define SourceDir "..\..\..\ausgabe\Bambus Launcher"
#endif

[Setup]
AppId={{8F3C2A51-6B7D-4E2A-9C1B-B4A3D5E6F701}
AppName={#AppName}
AppVersion={#AppVersion}
AppVerName={#AppName} {#AppVersion}
AppPublisher=Bambus
DefaultDirName={localappdata}\Programs\Bambus Launcher
DefaultGroupName=Bambus Client
DisableProgramGroupPage=yes
DisableDirPage=auto
PrivilegesRequired=lowest
OutputBaseFilename=Bambus-Launcher-Setup
SetupIconFile=..\bambus.ico
UninstallDisplayIcon={app}\{#AppExe}
UninstallDisplayName={#AppName}
WizardStyle=modern
WizardImageFile=wizard-gross.bmp
WizardSmallImageFile=wizard-klein.bmp
WizardImageStretch=yes
Compression=lzma2/max
SolidCompression=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
CloseApplications=yes

[Languages]
Name: "de"; MessagesFile: "compiler:Languages\German.isl"

[Messages]
WelcomeLabel1=Willkommen beim Bambus Client
WelcomeLabel2=Dieses Programm installiert den Bambus Launcher {#AppVersion} auf deinem Computer.%n%nDu brauchst nur Minecraft Java Edition – Java ist schon eingebaut.%n%nKlicke auf „Weiter“.
FinishedHeadingLabel=Fertig – viel Spaß!
FinishedLabel=Der Bambus Launcher ist installiert. Du findest ihn im Startmenü und auf dem Desktop.%n%nSO KOMMT ER IN DIE TASKLEISTE:%nStarte den Launcher, klicke unten in der Taskleiste mit der rechten Maustaste auf das Bambus-Symbol und wähle „An Taskleiste anheften“.

[Tasks]
Name: "desktop"; Description: "Verknüpfung auf dem Desktop erstellen"; GroupDescription: "Verknüpfungen:"

[Files]
Source: "{#SourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#AppName}"; Filename: "{app}\{#AppExe}"; Comment: "Bambus Client starten"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExe}"; Comment: "Bambus Client starten"; Tasks: desktop

[Run]
Filename: "{app}\{#AppExe}"; Description: "Bambus Launcher jetzt starten"; Flags: nowait postinstall skipifsilent
; Nach einem automatischen Update (still) den Launcher gleich wieder öffnen
Filename: "{app}\{#AppExe}"; Flags: nowait; Check: WizardSilent
