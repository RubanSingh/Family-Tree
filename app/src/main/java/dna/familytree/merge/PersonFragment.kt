package dna.familytree.merge

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dna.familytree.*
import dna.familytree.constant.Gender
import dna.familytree.databinding.MergePersonFragmentBinding
import dna.familytree.util.FileUtils
import org.folg.gedcom.model.Gedcom
import org.folg.gedcom.model.Person

open class PersonFragment : Fragment(R.layout.merge_person_fragment) {

    private val TAG: String = "PersonFragment"
    private lateinit var binding: MergePersonFragmentBinding
    private val model: MergeViewModel by activityViewModels()
    private lateinit var person: Person
    var data = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val position = requireArguments().getBoolean("position")
        person = model.getPerson(position)

        binding = MergePersonFragmentBinding.bind(view)
        binding.person = this

        // Border
        if (Gender.isMale(person)) binding.mergeBorder.setBackgroundResource(R.drawable.male_bg)
        else if (Gender.isFemale(person)) binding.mergeBorder.setBackgroundResource(R.drawable.female_bg)
        // Image
        val gedcom: Gedcom = if (position) model.secondGedcom else model.firstGedcom
        val treeId: Int = if (position) model.secondNum.value!! else model.firstNum
        FileUtils.showMainImageForPerson(gedcom, treeId, person, binding.mergeImage)
        // Death ribbon
        binding.mergeMourn.visibility = if (AppUtils.isDead(person)) View.VISIBLE else View.GONE
        // Personal events
        for (event in person.eventsFacts) {
            if (event.tag == "SEX") continue
            data += "<b>${ProfileFactsFragment.writeEventTitle(event)}</b><br>"
            if (event.value != null) {
                data += if (event.value == "Y") "${getString(R.string.yes)} "
                else "${event.value} "
            }
            if (event.date != null) data += "${GedcomDateConverter(event.date).writeDateLong()} "
            if (event.place != null) data += "${event.place} "
            if (event.address != null) data += "${DetailController.writeAddress(event.address, true)} "
            if (event.cause != null) data += event.cause
            data += "<br>"
        }
        if (data.any()) data = data.substring(0, data.length - 4)
        binding.mergeData.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(data, Html.FROM_HTML_MODE_COMPACT)
        else
            Html.fromHtml(data)

    }

    fun getName(): String {
        return AppUtils.firstAndLastName(person.names[0], " ")
    }
}
