    package com.simats.pddmate

    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.Button
    import android.widget.ImageView
    import android.widget.TextView
    import androidx.recyclerview.widget.RecyclerView

    class EnrollmentAdapter(
        private val slots: MutableList<EnrollmentSlot>,
        private val onEnrollClick: (EnrollmentSlot) -> Unit
    ) : RecyclerView.Adapter<EnrollmentAdapter.SlotViewHolder>() {

        private var singleEnrollment: Boolean = false

        fun setSingleEnrollment(value: Boolean) {
            singleEnrollment = value
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_enrollment_card, parent, false)
            return SlotViewHolder(view)
        }

        override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
            val slot = slots[position]
            holder.bind(slot)
        }

        override fun getItemCount(): Int = slots.size

        inner class SlotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val codeTitle = view.findViewById<TextView>(R.id.enrollmentCardCodeTitle)
            private val mentor = view.findViewById<TextView>(R.id.enrollmentCardMentor)
            private val slotsLeft = view.findViewById<TextView>(R.id.enrollmentCardSlotsLeft)
            private val category = view.findViewById<TextView>(R.id.enrollmentCardCategory)
            private val icon = view.findViewById<ImageView>(R.id.enrollmentCardIcon)
            private val enrollButton = view.findViewById<Button>(R.id.enrollButton)
            private val statusText = view.findViewById<TextView>(R.id.enrollmentCardStatusText)

            fun bind(slot: EnrollmentSlot) {
                codeTitle.text = codeTitle.context.getString(
                    R.string.text_code_title,
                    slot.courseCode,
                    slot.title
                )
                slotsLeft.text = slotsLeft.context.getString(
                    R.string.text_slots_left,
                    slot.slotsLeft,
                    slot.capacity
                )
                mentor.text = slot.mentorName
                category.text = slot.type

                // Default UI logic for status
                when (slot.status) {
                    "Pending" -> {
                        enrollButton.visibility = View.GONE
                        statusText.visibility = View.VISIBLE
                        statusText.text = "Status: Pending"
                    }
                    "Approved", "Accepted" -> {
                        enrollButton.visibility = View.GONE
                        statusText.visibility = View.VISIBLE
                        statusText.text = "Status: Accepted"
                    }
                    else -> {
                        enrollButton.visibility = View.VISIBLE
                        statusText.visibility = View.GONE
                        // Disable enroll button if already enrolled in ANY slot
                        enrollButton.isEnabled = !singleEnrollment
                    }
                }

                enrollButton.setOnClickListener {
                    if (enrollButton.isEnabled) {
                        val context = enrollButton.context
                        val dialogView = LayoutInflater.from(context)
                            .inflate(R.layout.activity_confirm_enrollment_popup, null)
                        val bodyMessage = dialogView.findViewById<TextView>(R.id.body_message)
                        bodyMessage.text = context.getString(
                            R.string.confirm_enrollment_message_dynamic,
                            slot.title
                        )
                        val dialog = android.app.AlertDialog.Builder(context)
                            .setView(dialogView)
                            .create()
                        dialogView.findViewById<ImageView>(R.id.close_icon).setOnClickListener {
                            dialog.dismiss()
                        }
                        dialogView.findViewById<Button>(R.id.btn_confirm).setOnClickListener {
                            onEnrollClick(slot)
                            dialog.dismiss()
                        }
                        dialog.show()
                    }
                }
            }
        }
    }
