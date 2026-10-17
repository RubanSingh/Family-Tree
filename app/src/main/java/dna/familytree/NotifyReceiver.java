/*  This BroadcastReceiver has a double function:
	- Receive intent from Notifier to create notifications
	- Receive ACTION_BOOT_COMPLETED after reboot to restore notifications saved in settings.json
*/

package dna.familytree;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class NotifyReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// Set again alarms after reboot
		if( Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ) {

			new Notifier(context, null, 0, Notifier.What.REBOOT);
			dna.familytree.feature.reminders.ReminderScheduler.reschedule(context);

		} else { // Create notification

			Intent notifyIntent = new Intent(context, TreesController.class)
					.putExtra(Notifier.TREE_ID_KEY, intent.getIntExtra("treeId", 0))
					.putExtra(Notifier.INDI_ID_KEY, intent.getStringExtra("indiId"))
					.putExtra(Notifier.NOTIFY_ID_KEY, intent.getIntExtra("id", 1));
			PendingIntent pendingIntent = PendingIntent.getActivity(context, intent.getIntExtra("id", 1),
					notifyIntent, PendingIntent.FLAG_IMMUTABLE);

			NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Notifier.CHANNEL_ID)
					.setSmallIcon(R.drawable.tree)
					.setContentTitle(intent.getStringExtra("title"))
					.setContentText(intent.getStringExtra("text"))
					.setContentIntent(pendingIntent)
					.setAutoCancel(true)
					.setCategory(NotificationCompat.CATEGORY_EVENT);

			NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
			// From Android 13 notifications need the user's permission: without it nothing can be shown
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context,
					Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
				return;
			notificationManager.notify(intent.getIntExtra("id", 1), builder.build());
		}
	}
}
