package dna.familytree.util;

import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Created by rubansingh.john on 6/16/2021.
 */
public abstract class AnalyticsUtil {

    private static final String EVENT_NEW_TREE_ID = "new_tree";
    private static final String EVENT_NEW_TREE_NAME = "New Tree";

    private static final String EVENT_SETTINGS_ABOUT_ID = "settings_about";
    private static final String EVENT_SETTINGS_ABOUT_NAME = "SettingsAbout";

    private static final String EVENT_SETTINGS_AUTO_SAVE_ID = "settings_auto_save";
    private static final String EVENT_SETTINGS_AUTO_SAVE_NAME = "SettingsAutoSave";

    private static final String EVENT_SETTINGS_LAST_LOAD_ID = "settings_last_load";
    private static final String EVENT_SETTINGS_LAST_LOAD_NAME = "SettingsLastLoad";
    private static final String EVENT_SETTINGS_EXPERT_MODE_ID = "settings_expert_mode";
    private static final String EVENT_SETTINGS_EXPERT_MODE_NAME = "SettingsExpertMode";
    private static final String EVENT_SETTINGS_LANGUAGE_ID = "settings_language";
    private static final String EVENT_SETTINGS_LANGUAGE_NAME = "SettingsLanguage";
    private static final String EVENT_NEWTREE_CREATE_TREE_ID = "create_tree";
    private static final String EVENT_NEWTREE_CREATE_TREE_NAME = "CreateTree";
    private static final String EVENT_NEWTREE_DOWNLOAD_TREE_ID = "download_tree";
    private static final String EVENT_NEWTREE_DOWNLOAD_TREE_NAME = "DownloadTree";
    private static final String EVENT_NEWTREE_IMPORT_GEDCOM_ID = "import_gedcom";
    private static final String EVENT_NEWTREE_IMPORT_GEDCOM_NAME = "ImportGEDCOM";
    private static final String EVENT_NEWTREE_RECOVER_BACKUP_ID = "recover_backup";
    private static final String EVENT_NEWTREE_RECOVER_BACKUP_NAME = "RecoverBackup";
    private static final String EVENT_TREES_INFO_ID = "info_tree";
    private static final String EVENT_TREES_INFO_NAME = "InfoTree";
    private static final String EVENT_TREES_RENAME_TREE_ID = "rename_tree";
    private static final String EVENT_TREES_RENAME_TREE_NAME = "RenameTree";
    private static final String EVENT_TREES_MEDIA_FOLDER_ID = "media_folder";
    private static final String EVENT_TREES_MEDIA_FOLDER_NAME = "OpenMediaFolder";
    private static final String EVENT_TREES_FIND_ERROR_ID = "find_error";
    private static final String EVENT_TREES_FIND_ERROR_NAME = "FindError";
    private static final String EVENT_TREES_SHARE_TREE_ID = "share_tree";
    private static final String EVENT_TREES_SHARE_TREE_NAME = "ShareTree";
    private static final String EVENT_TREES_DELETE_TREE_ID = "delete_tree";
    private static final String EVENT_TREES_DELETE_TREE_NAME = "DeleteTree";
    private static final String EVENT_TREES_BACKUP_TREE_ID = "backup_tree";
    private static final String EVENT_TREES_BACKUP_TREE_NAME = "BackupTree";
    private static final String EVENT_TREES_EXPORT_TREE_ID = "export_tree";
    private static final String EVENT_TREES_EXPORT_TREE_NAME = "ExportTree";

    private static final String EVENT_DIAGRAM_FIRST_PERSON_ID = "first_person";
    private static final String EVENT_DIAGRAM_FIRST_PERSON_NAME = "AddFirstPerson";
    private static final String EVENT_DIAGRAM_EDIT_PROFILE_ID = "edit_profile";
    private static final String EVENT_DIAGRAM_EDIT_PROFILE_NAME = "EditProfile";
    private static final String EVENT_PERSON_ADD_PERSON_ID = "add_person";
    private static final String EVENT_PERSON_ADD_PERSON_NAME = "AddPerson";
    private static final String EVENT_PERSON_EDIT_ID_ID = "edit_id_profile";
    private static final String EVENT_PERSON_EDIT_ID_NAME = "EditIDProfile";
    private static final String EVENT_PERSON_OPEN_DIAGRAM_ID = "open_family_diagram";
    private static final String EVENT_PERSON_OPEN_DIAGRAM_NAME = "OpenFamilyDiagram";
    private static final String EVENT_DIAGRAM_LINK_NEW_PERSON_ID = "link_new_person";
    private static final String EVENT_DIAGRAM_LINK_NEW_PERSON_NAME = "LinkNewPerson";
    private static final String EVENT_DIAGRAM_LINK_EXISTING_PERSON_ID = "link_existing_person";
    private static final String EVENT_DIAGRAM_LINK_EXISTING_PERSON_NAME = "LinkExistingPerson";
    private static final String EVENT_DIAGRAM_OPEN_FAMILY_ID = "open_family";
    private static final String EVENT_DIAGRAM_OPEN_FAMILY_NAME = "OpenFamily";
    private static final String EVENT_PERSONS_SORT_SURNAME_ID = "persons_sort_surname";
    private static final String EVENT_PERSONS_SORT_SURNAME_NAME = "PersonSortSurname";
    private static final String EVENT_PERSONS_SORT_DATE_ID = "persons_sort_date";
    private static final String EVENT_PERSONS_SORT_DATE_NAME = "PersonSortDate";
    private static final String EVENT_PERSONS_SORT_AGE_ID = "persons_sort_age";
    private static final String EVENT_PERSONS_SORT_AGE_NAME = "PersonSortAge";
    private static final String EVENT_PERSONS_SORT_RELATIVES_ID = "persons_sort_relatives";
    private static final String EVENT_PERSONS_SORT_RELATIVES_NAME = "PersonSortRelatives";

    private static final String EVENT_PURCHASE_COMPLETED_ID = "purchase_completed";
    private static final String EVENT_PURCHASE_COMPLETED_NAME = "PurchaseCompleted";
    private static final String EVENT_FAMILY_DELETE_ID = "family_delete";
    private static final String EVENT_FAMILY_DELETE_NAME = "FamilyDelete";
    private static final String EVENT_FAMILY_EDIT_ID = "family_edit";
    private static final String EVENT_FAMILY_EDIT_NAME = "FamilyEdit";
    private static final String EVENT_FAMILY_ADD_ID = "family_add";
    private static final String EVENT_FAMILY_ADD_NAME = "FamilyAdd";
    private static final String EVENT_MEDIA_EDIT_ID = "media_edit";
    private static final String EVENT_MEDIA_EDIT_NAME = "MediaEdit";
    private static final String EVENT_MEDIA_ADD_ID = "media_add";
    private static final String EVENT_MEDIA_ADD_NAME = "MediaAdd";
    private static final String EVENT_MEDIA_DELETE_ID = "media_delete";
    private static final String EVENT_MEDIA_DELETE_NAME = "MediaDelete";
    private static final String EVENT_DIAGRAM_SETTINGS_ADD_PARTNER_ID = "settings_add_partner";
    private static final String EVENT_DIAGRAM_SETTINGS_ADD_PARTNER_NAME = "SettingsAddPartner";
    private static final String EVENT_DIAGRAM_OPEN_SETTINGS_ID = "open_settings_diagram";
    private static final String EVENT_DIAGRAM_OPEN_SETTINGS_NAME = "OpenSettingsDiagram";
    private static final String EVENT_DIAGRAM_EXPORT_PDF_ID = "export_pdf_tree";
    private static final String EVENT_DIAGRAM_EXPORT_PDF_NAME = "ExportPDFTree";
    private static final String EVENT_DIAGRAM_UNLINK_PERSON_ID = "unlink_person";
    private static final String EVENT_DIAGRAM_UNLINK_PERSON_NAME = "UnLinkPerson";
    private static final String EVENT_DIAGRAM_DELETE_PERSON_ID = "delete_person";
    private static final String EVENT_DIAGRAM_DELETE_PERSON_NAME = "DeletePerson";
    private static final String EVENT_DIAGRAM_LINK_PROFILE_ID = "profile";
    private static final String EVENT_DIAGRAM_LINK_PROFILE_NAME = "OpenProfile";

    private static final String EVENT_CHANGE_THEME_ID = "change_app_theme";
    private static final String EVENT_CHANGE_THEME_NAME = "ChangeTheme";

    private static final String EVENT_CHANGE_DARK_THEME_ID = "change_dark_theme";
    private static final String EVENT_CHANGE_DARK_THEME_NAME = "ChangeDarkTheme";

    private static final String EVENT_PREMIUM_ID = "action_premium";
    private static final String EVENT_PREMIUM_NAME = "ActionPremium";

    private static final String EVENT_MERGE_TREE_ID = "merge_tree";
    private static final String EVENT_MERGE_TREE_NAME = "MergeTree";

    private static final String EVENT_FEEDBACK_ID = "settings_feedback";
    private static final String EVENT_FEEDBACK_NAME = "SettingsFeedback";

    private static final String EVENT_RATE_ID = "settings_rate";
    private static final String EVENT_RATE_NAME = "SettingsRate";

    private static final String EVENT_SHARE_ID = "settings_share";
    private static final String EVENT_SHARE_NAME = "SettingsShare";

    //Screen Events
    private static final String EVENT_CONTROLLER_TREE_LIST_ID = "tree_list_controller";
    private static final String EVENT_CONTROLLER_TREE_LIST_NAME = "TreeListController";

    private static final String EVENT_CONTROLLER_SETTINGS_ID = "settings_controller";
    private static final String EVENT_CONTROLLER_SETTINGS_NAME = "SettingsController";

    private static final String EVENT_CONTROLLER_PRINCIPAL_ID = "principal_controller";
    private static final String EVENT_CONTROLLER_PRINCIPAL_NAME = "PrincipalController";

    private static final String EVENT_CONTROLLER_PERSON_EDITOR_ID = "person_editor_controller";
    private static final String EVENT_CONTROLLER_PERSON_EDITOR_NAME = "PersonEditorController";

    private static final String EVENT_CONTROLLER_MEDIA_FOLDER_ID = "media_folder_controller";
    private static final String EVENT_CONTROLLER_MEDIA_FOLDER_NAME = "MediaFolderController";

    private static final String EVENT_CONTROLLER_NEW_TREE_ID = "new_tree_controller";
    private static final String EVENT_CONTROLLER_NEW_TREE_NAME = "NewTreeController";

    private static final String EVENT_CONTROLLER_INFO_ID = "info_controller";
    private static final String EVENT_CONTROLLER_INFO_NAME = "InfoController";

    private static final String EVENT_CONTROLLER_LAUNCHER_ID = "launcher_controller";
    private static final String EVENT_CONTROLLER_LAUNCHER_NAME = "LauncherController";

    private static final String EVENT_CONTROLLER_IMAGE_ID = "image_controller";
    private static final String EVENT_CONTROLLER_IMAGE_NAME = "ImageController";

    private static final String EVENT_CONTROLLER_DIAGRAM_SETTINGS_ID = "diagram_settings_controller";
    private static final String EVENT_CONTROLLER_DIAGRAM_SETTINGS_NAME = "DiagramSettingsController";

    private static final String EVENT_CONTROLLER_DETAILS_ID = "details_controller";
    private static final String EVENT_CONTROLLER_DETAILS_NAME = "DetailsController";

    private static final String EVENT_CONTROLLER_ABOUT_ID = "about_controller";
    private static final String EVENT_CONTROLLER_ABOUT_NAME = "AboutController";

    private static final String EVENT_CONTROLLER_SHARING_ID = "sharing_controller";
    private static final String EVENT_CONTROLLER_SHARING_NAME = "SharingController";

    private static final String EVENT_CONTROLLER_PROCESS_ID = "process_controller";
    private static final String EVENT_CONTROLLER_PROCESS_NAME = "ProcessController";

    private static final String EVENT_CONTROLLER_CONFIRMATION_ID = "confirmation_controller";
    private static final String EVENT_CONFIRMATION_NAME = "ConfirmationController";

    private static final String EVENT_CONTROLLER_COMPARE_ID = "compare_controller";
    private static final String EVENT_CONTROLLER_COMPARE_NAME = "CompareController";

    private static final String EVENT_CONTROLLER_SUBMITTER_ID = "submitter_controller";
    private static final String EVENT_CONTROLLER_SUBMITTER_NAME = "SubmitterController";

    private static final String EVENT_CONTROLLER_SOURCE_CITATION_ID = "source_citation_controller";
    private static final String EVENT_CONTROLLER_SOURCE_CITATION_NAME = "SourceCitationController";

    private static final String EVENT_CONTROLLER_SOURCE_ID = "source_controller";
    private static final String EVENT_CONTROLLER_SOURCE_NAME = "SourceController";

    private static final String EVENT_CONTROLLER_REPOSITORY_REF_ID = "repository_ref_controller";
    private static final String EVENT_CONTROLLER_REPOSITORY_REF_NAME = "RepositoryRefController";

    private static final String EVENT_CONTROLLER_REPOSITORY_ID = "repository_controller";
    private static final String EVENT_CONTROLLER_REPOSITORY_NAME = "RepositoryController";

    private static final String EVENT_CONTROLLER_NOTE_ID = "note_controller";
    private static final String EVENT_CONTROLLER_NOTE_NAME = "NoteController";

    private static final String EVENT_CONTROLLER_NAME_ID = "name_controller";
    private static final String EVENT_CONTROLLER_NAME_NAME = "NameController";

    private static final String EVENT_CONTROLLER_MEDIA_ID = "media_controller";
    private static final String EVENT_CONTROLLER_MEDIA_NAME = "MediaController";

    private static final String EVENT_CONTROLLER_FAMILY_ID = "family_controller";
    private static final String EVENT_CONTROLLER_FAMILY_NAME = "FamilyController";

    private static final String EVENT_CONTROLLER_EXTENSION_ID = "extension_controller";
    private static final String EVENT_CONTROLLER_EXTENSION_NAME = "ExtensionController";

    private static final String EVENT_CONTROLLER_EVENT_ID = "event_controller";
    private static final String EVENT_CONTROLLER_EVENT_NAME = "EventController";

    private static final String EVENT_CONTROLLER_CHANGE_ID = "change_controller";
    private static final String EVENT_CONTROLLER_CHANGE_NAME = "ChangeController";
    private static final String EVENT_CONTROLLER_ADDRESS_ID = "address_controller";
    private static final String EVENT_CONTROLLER_ADDRESS_NAME = "AddressController";


    //Fragment Screen Events
    private static final String EVENT_FRAGMENT_PROFILE_RELATIVES_ID = "profile_relatives_fragment";
    private static final String EVENT_FRAGMENT_PROFILE_RELATIVES_NAME = "ProfileRelativesFragment";

    private static final String EVENT_FRAGMENT_PROFILE_FACTS_ID = "profile_facts_fragment";
    private static final String EVENT_FRAGMENT_PROFILE_FACTS_NAME = "ProfileFactsFragment";

    private static final String EVENT_FRAGMENT_PROFILE_MEDIA_ID = "profile_media_fragment";
    private static final String EVENT_FRAGMENT_PROFILE_MEDIA_NAME = "ProfileMediaFragment";

    private static final String EVENT_FRAGMENT_DIAGRAM_ID = "diagram_fragment";
    private static final String EVENT_FRAGMENT_DIAGRAM_NAME = "DiagramFragment";

    private static final String EVENT_FRAGMENT_SUBMITTERS_ID = "submitters_fragment";
    private static final String EVENT_FRAGMENT_SUBMITTERS_NAME = "SubmittersFragment";

    private static final String EVENT_FRAGMENT_SOURCE_ID = "source_fragment";
    private static final String EVENT_FRAGMENT_SOURCE_NAME = "SourceFragment";

    private static final String EVENT_FRAGMENT_REPOSITORIES_ID = "repositories_fragment";
    private static final String EVENT_FRAGMENT_REPOSITORIES_NAME = "RepositoriesFragment";

    private static final String EVENT_FRAGMENT_PERSONS_ID = "persons_fragment";
    private static final String EVENT_FRAGMENT_PERSONS_NAME = "PersonsFragment";

    private static final String EVENT_FRAGMENT_NOTES_ID = "notes_fragment";
    private static final String EVENT_FRAGMENT_NOTES_NAME = "NotesFragment";

    private static final String EVENT_FRAGMENT_MEDIA_ID = "media_fragment";
    private static final String EVENT_FRAGMENT_MEDIA_NAME = "MediaFragment";

    private static final String EVENT_FRAGMENT_FAMILY_ID = "family_fragment";
    private static final String EVENT_FRAGMENT_FAMILY_NAME = "FamilyFragment";


    /**
     * Log Event for creating a new Tree.
     */
    public static void logEventNewTree(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_NEW_TREE_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_NEW_TREE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event for open Settings About.
     */
    public static void logEventSettingsAbout(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_SETTINGS_ABOUT_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_SETTINGS_ABOUT_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event for open SettingsAutoSave.
     */
    public static void logEventSettingsAutoSave(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_SETTINGS_AUTO_SAVE_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_SETTINGS_AUTO_SAVE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event for open SettingsLastLoad.
     */
    public static void logEventSettingsLastLoad(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_SETTINGS_LAST_LOAD_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_SETTINGS_LAST_LOAD_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event for open SettingsExpertMode.
     */
    public static void logEventSettingsExpertMode(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_SETTINGS_EXPERT_MODE_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_SETTINGS_EXPERT_MODE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event for open SettingsLanguage.
     */
    public static void logEventSettingsLanguage(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_SETTINGS_LANGUAGE_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_SETTINGS_LANGUAGE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event for Create Tree.
     */
    public static void logEventCreateTree(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_NEWTREE_CREATE_TREE_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_NEWTREE_CREATE_TREE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event for Download Tree.
     */
    public static void logEventDownloadTree(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_NEWTREE_DOWNLOAD_TREE_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_NEWTREE_DOWNLOAD_TREE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event for Import GEDCOM.
     */
    public static void logEventImportGEDCOM(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_NEWTREE_IMPORT_GEDCOM_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_NEWTREE_IMPORT_GEDCOM_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event for Recover Backup.
     */
    public static void logEventRecoverBackup(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_NEWTREE_RECOVER_BACKUP_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_NEWTREE_RECOVER_BACKUP_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event open Info Tree.
     */
    public static void logEventInfoTree(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_TREES_INFO_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_TREES_INFO_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event Rename Tree.
     */
    public static void logEventRenameTree(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_TREES_RENAME_TREE_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_TREES_RENAME_TREE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event Media Folders.
     */
    public static void logEventMediaFolders(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_TREES_MEDIA_FOLDER_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_TREES_MEDIA_FOLDER_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event Find Errors.
     */
    public static void logEventFindErrors(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_TREES_FIND_ERROR_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_TREES_FIND_ERROR_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event Share Tree.
     */
    public static void logEventShareTree(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_TREES_SHARE_TREE_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_TREES_SHARE_TREE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event Export Tree.
     */
    public static void logEventExportTree(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_TREES_EXPORT_TREE_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_TREES_EXPORT_TREE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event Backup Tree.
     */
    public static void logEventBackupTree(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_TREES_BACKUP_TREE_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_TREES_BACKUP_TREE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event Delete Tree.
     */
    public static void logEventDeleteTree(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_TREES_DELETE_TREE_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_TREES_DELETE_TREE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event FirstPerson.
     */
    public static void logEventAddFirstPerson(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_DIAGRAM_FIRST_PERSON_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_DIAGRAM_FIRST_PERSON_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event PersonEdit.
     */
    public static void logEventPersonEditProfile(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_DIAGRAM_EDIT_PROFILE_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_DIAGRAM_EDIT_PROFILE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event AddPerson.
     */
    public static void logEventAddPerson(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_PERSON_ADD_PERSON_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_PERSON_ADD_PERSON_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event PersonEditId.
     */
    public static void logEventPersonEditIdProfile(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_PERSON_EDIT_ID_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_PERSON_EDIT_ID_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event OpenDiagram.
     */
    public static void logEventOpenDiagram(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_PERSON_OPEN_DIAGRAM_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_PERSON_OPEN_DIAGRAM_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event PersonDelete.
     */
    public static void logEventPersonDeleteProfile(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_DIAGRAM_DELETE_PERSON_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_DIAGRAM_DELETE_PERSON_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event LinkNewPerson.
     */
    public static void logEventLinkNewPerson(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_DIAGRAM_LINK_NEW_PERSON_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_DIAGRAM_LINK_NEW_PERSON_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event LinkExistingPerson.
     */
    public static void logEventLinkExistingPerson(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_DIAGRAM_LINK_EXISTING_PERSON_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_DIAGRAM_LINK_EXISTING_PERSON_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event Open Family.
     */
    public static void logEventOpenFamily(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_DIAGRAM_OPEN_FAMILY_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_DIAGRAM_OPEN_FAMILY_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event SortSurname.
     */
    public static void logEventSortSurname(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_PERSONS_SORT_SURNAME_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_PERSONS_SORT_SURNAME_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event SortDate.
     */
    public static void logEventSortDate(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_PERSONS_SORT_DATE_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_PERSONS_SORT_DATE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event SortAge.
     */
    public static void logEventSortAge(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_PERSONS_SORT_AGE_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_PERSONS_SORT_AGE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event SortRelatives.
     */
    public static void logEventSortRelatives(FirebaseAnalytics firebaseAnalytics) {

    }

    /**
     * Log Event Purchase Completion.
     */
    public static void logEventSortPurchaseCompleted(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_PURCHASE_COMPLETED_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_PURCHASE_COMPLETED_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event DeleteFamily.
     */
    public static void logEventDeleteFamily(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_FAMILY_DELETE_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_FAMILY_DELETE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event AddFamily.
     */
    public static void logEventAddFamily(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_FAMILY_ADD_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_FAMILY_ADD_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event EditFamily.
     */
    public static void logEventEditFamily(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_FAMILY_EDIT_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_FAMILY_EDIT_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event AddMedia.
     */
    public static void logEventAddMedia(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_MEDIA_ADD_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_MEDIA_ADD_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event EditMedia.
     */
    public static void logEventEditMedia(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_MEDIA_EDIT_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_MEDIA_EDIT_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event DeleteMedia.
     */
    public static void logEventDeleteMedia(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_MEDIA_DELETE_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_MEDIA_DELETE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event SettingsAddPartner.
     */
    public static void logEventSettingsAddPartner(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_DIAGRAM_SETTINGS_ADD_PARTNER_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_DIAGRAM_SETTINGS_ADD_PARTNER_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event OpenSettingsDiagram.
     */
    public static void logEventOpenSettingsDiagram(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_DIAGRAM_OPEN_SETTINGS_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_DIAGRAM_OPEN_SETTINGS_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event ExportPDFTree.
     */
    public static void logEventExportPDFTree(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_DIAGRAM_EXPORT_PDF_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_DIAGRAM_EXPORT_PDF_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event UnLinkPerson.
     */
    public static void logEventUnLinkNewPerson(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_DIAGRAM_UNLINK_PERSON_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_DIAGRAM_UNLINK_PERSON_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event Profile.
     */
    public static void logEventOpenProfile(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, EVENT_DIAGRAM_LINK_PROFILE_ID);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, EVENT_DIAGRAM_LINK_PROFILE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    /**
     * Log Event for TreeList Activity.
     */
    public static void logEventTreeListActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_TREE_LIST_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_TREE_LIST_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Settings Activity.
     */
    public static void logEventSettingsActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_SETTINGS_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_SETTINGS_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Principal Activity.
     */
    public static void logEventPrincipalActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_PRINCIPAL_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_PRINCIPAL_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for PersonEditor Activity.
     */
    public static void logEventPersonEditorActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_PERSON_EDITOR_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_PERSON_EDITOR_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for MediaFolder Activity.
     */
    public static void logEventMediaFolderActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_MEDIA_FOLDER_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_MEDIA_FOLDER_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for NewTree Activity.
     */
    public static void logEventNewTreeActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_NEW_TREE_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_NEW_TREE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Info Activity.
     */
    public static void logEventInfoActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_INFO_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_INFO_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Launcher Activity.
     */
    public static void logEventLauncherActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_LAUNCHER_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_LAUNCHER_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Image Activity.
     */
    public static void logEventImageActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_IMAGE_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_IMAGE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for DiagramSettings Activity.
     */
    public static void logEventDiagramSettingsActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_DIAGRAM_SETTINGS_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_DIAGRAM_SETTINGS_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Details Activity.
     */
    public static void logEventDetailsActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_DETAILS_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_DETAILS_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for About Activity.
     */
    public static void logEventAboutActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_ABOUT_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_ABOUT_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Sharing Activity.
     */
    public static void logEventSharingActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_SHARING_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_SHARING_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Process Activity.
     */
    public static void logEventProcessActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_PROCESS_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_PROCESS_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Confirmation Activity.
     */
    public static void logEventConfirmationActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_CONFIRMATION_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONFIRMATION_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Compare Activity.
     */
    public static void logEventCompareActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_COMPARE_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_COMPARE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Submitter Activity.
     */
    public static void logEventSubmitterActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_SUBMITTER_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_SUBMITTER_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Source Citation Activity.
     */
    public static void logEventSourceCitationActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_SOURCE_CITATION_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_SOURCE_CITATION_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Source Activity.
     */
    public static void logEventSourceActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_SOURCE_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_SOURCE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for RepositoryRef Activity.
     */
    public static void logEventRepositoryRefActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_REPOSITORY_REF_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_REPOSITORY_REF_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Repository Activity.
     */
    public static void logEventRepositoryActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_REPOSITORY_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_REPOSITORY_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Note Activity.
     */
    public static void logEventNoteActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_NOTE_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_NOTE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Name Activity.
     */
    public static void logEventNameActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_NAME_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_NAME_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Media Activity.
     */
    public static void logEventMediaActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_MEDIA_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_MEDIA_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Family Activity.
     */
    public static void logEventFamilyActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_FAMILY_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_FAMILY_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Extension Activity.
     */
    public static void logEventExtensionActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_EXTENSION_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_EXTENSION_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Event Activity.
     */
    public static void logEventEventActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_EVENT_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_EVENT_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Change Activity.
     */
    public static void logEventChangeActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_CHANGE_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_CHANGE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Address Activity.
     */
    public static void logEventAddressActivity(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CONTROLLER_ADDRESS_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CONTROLLER_ADDRESS_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for ProfileRelatives Fragment.
     */
    public static void logProfileRelativesFragment(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_FRAGMENT_PROFILE_RELATIVES_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_FRAGMENT_PROFILE_RELATIVES_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for ProfileMedia Fragment.
     */
    public static void logProfileMediaFragment(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_FRAGMENT_PROFILE_MEDIA_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_FRAGMENT_PROFILE_MEDIA_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for ProfileFacts Fragment.
     */
    public static void logProfileFactsFragment(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_FRAGMENT_PROFILE_FACTS_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_FRAGMENT_PROFILE_FACTS_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Diagram Fragment.
     */
    public static void logDiagramFragment(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_FRAGMENT_DIAGRAM_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_FRAGMENT_DIAGRAM_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Submitters Fragment.
     */
    public static void logSubmittersFragment(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_FRAGMENT_SUBMITTERS_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_FRAGMENT_SUBMITTERS_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Source Fragment.
     */
    public static void logSourceFragment(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_FRAGMENT_SOURCE_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_FRAGMENT_SOURCE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Repositories Fragment.
     */
    public static void logRepositoriesFragment(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_FRAGMENT_REPOSITORIES_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_FRAGMENT_REPOSITORIES_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Persons Fragment.
     */
    public static void logPersonsFragment(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_FRAGMENT_PERSONS_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_FRAGMENT_PERSONS_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Notes Fragment.
     */
    public static void logNotesFragment(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_FRAGMENT_NOTES_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_FRAGMENT_NOTES_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Media Fragment.
     */
    public static void logMediaFragment(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_FRAGMENT_MEDIA_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_FRAGMENT_MEDIA_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    /**
     * Log Event for Family Fragment.
     */
    public static void logFamilyFragment(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_FRAGMENT_FAMILY_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_FRAGMENT_FAMILY_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    public static void logEventChangeApplicationTheme(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CHANGE_THEME_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CHANGE_THEME_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    public static void logEventDarkTheme(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_CHANGE_DARK_THEME_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_CHANGE_DARK_THEME_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    public static void logEventPremiumAction(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_PREMIUM_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_PREMIUM_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    public static void logEventMergeTree(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_MERGE_TREE_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_MERGE_TREE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    public static void logEventSettingsFeedBack(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_FEEDBACK_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_FEEDBACK_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    public static void logEventSettingsRateApp(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_RATE_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_RATE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    public static void logEventSettingsShare(FirebaseAnalytics firebaseAnalytics) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, EVENT_SHARE_ID);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, EVENT_SHARE_NAME);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }
}
