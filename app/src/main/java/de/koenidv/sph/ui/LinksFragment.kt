package de.koenidv.sph.ui

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.adapters.LinksAdapter
import de.koenidv.sph.database.TilesDb
import de.koenidv.sph.objects.Tile


class LinksFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_links, container, false)

        val linksRecycler = view.findViewById<RecyclerView>(R.id.linksRecycler)

        // Get all tiles that are not displayed within the app itself
        val tiles = TilesDb.getInstance().getTilesByType("other").sortedBy { it.name }.toMutableList()
        // Add start page item at the end of the list. Might remove this later
        val colorValue = TypedValue()
        requireContext().theme.resolveAttribute(R.attr.colorPrimary, colorValue, true)
        tiles.add(Tile(
                getString(R.string.tile_startpage),
                "https://start.schulportal.hessen.de/index.php",
                "start",
                "home",
                colorValue.data
        ))
        // Set up links recycler
        val linksAdapter = LinksAdapter(tiles) {
            // Open WebViewFragment with respective url on click
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.webviewAction, bundleOf("url" to it.location))
        }
        linksRecycler.layoutManager = LinearLayoutManager(requireContext())
        linksRecycler.adapter = linksAdapter

        return view
    }


}