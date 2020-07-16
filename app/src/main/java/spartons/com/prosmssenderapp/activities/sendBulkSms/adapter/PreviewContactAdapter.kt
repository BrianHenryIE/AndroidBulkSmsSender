package spartons.com.prosmssenderapp.activities.sendBulkSms.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import spartons.com.prosmssenderapp.activities.sendBulkSms.data.SmsContactMessage
import spartons.com.prosmssenderapp.activities.sendBulkSms.viewHolder.PreviewContactViewHolder


/**
 * Ahsen Saeed}
 * ahsansaeed067@gmail.com}
 * 6/26/19}
 */

class PreviewContactAdapter :
    ListAdapter<SmsContactMessage, PreviewContactViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PreviewContactViewHolder.create(parent)

    override fun onBindViewHolder(holder: PreviewContactViewHolder, position: Int) {
        holder.bind(getItem(position).contactNumber, getItem(position).message)
        holder.setClickListener {
            val tempList = currentList.toMutableList()
            tempList.removeAt(it)
            submitList(tempList)
        }
    }

    private companion object {

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SmsContactMessage>() {

            override fun areItemsTheSame(oldItem: SmsContactMessage, newItem: SmsContactMessage) = oldItem == newItem

            override fun areContentsTheSame(oldItem: SmsContactMessage, newItem: SmsContactMessage) = oldItem == newItem
        }
    }
}
