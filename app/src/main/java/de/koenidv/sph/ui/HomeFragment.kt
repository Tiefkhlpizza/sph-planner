package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.adapters.CompactPostsAdapter
import de.koenidv.sph.database.DatabaseHelper
import de.koenidv.sph.database.PostsDb
import de.koenidv.sph.objects.Post
import de.koenidv.sph.parsing.Utility


class HomeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_home, container, false)
        //val prefs = requireContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)

        // Set random greeting as action bar title, once per app start
        if (SphPlanner.randomGreeting == null) SphPlanner.randomGreeting = Utility().getGreeting()
        (activity as AppCompatActivity).supportActionBar?.title = SphPlanner.randomGreeting

        // Only for demonstration

        val surpriseButton = view.findViewById<Button>(R.id.surpriseButton)
        surpriseButton.setOnClickListener {
            //NetworkManager().indexAll { Toast.makeText(SphPlanner.applicationContext(), "Heute schon, Kartoffel", Toast.LENGTH_SHORT).show() }
            //NetworkManager().loadAndSavePosts { Toast.makeText(SphPlanner.applicationContext(), "Kartoffelfeld abgeräumt", Toast.LENGTH_SHORT).show() }
            DatabaseHelper.getInstance().writableDatabase.execSQL("DELETE FROM posts WHERE post_id IN (SELECT post_id FROM posts ORDER BY date DESC LIMIT 3)")
        }

        /*
         * Timetable
         */

        val timetable = view.findViewById<FragmentContainerView>(R.id.timetableFragment)
        view.findViewById<LinearLayout>(R.id.timetableLayout).setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.timetableFromHomeAction, null, null,
                            FragmentNavigatorExtras(timetable to "timetable"))
        }

        /*
         * Unread posts
         */

        val unreadPostsLayout = view.findViewById<LinearLayout>(R.id.unreadPostsLayout)
        val unreadPostsRecycler = view.findViewById<RecyclerView>(R.id.unreadPostsRecycler)
        val moreUnreadText = view.findViewById<TextView>(R.id.moreUnreadTextView)

        // Get unread posts
        // and up to 4 read posts
        // We'll only display 4 posts,
        // but unread posts will get removed once they are read
        val posts = PostsDb.getInstance().unread.toMutableList()
        var postsOverflow: Int? = null
        if (posts.size > 4) {
            postsOverflow = posts.size - 4
            // Display more unread posts text
            moreUnreadText.text = resources.getQuantityString(R.plurals.posts_more_unread, postsOverflow, postsOverflow)
            moreUnreadText.visibility = View.VISIBLE
        }
        posts.addAll(PostsDb.getInstance().getRead(4))

        unreadPostsRecycler.setHasFixedSize(true)
        unreadPostsRecycler.adapter = CompactPostsAdapter(posts, 4) { post: Post, _: View ->
            // Show single post bottom sheet
            PostSheet(post).show(parentFragmentManager, "post")
            // Remove the item from the list if it is unread
            if (post.unread) {
                val index = posts.indexOf(post)
                posts.removeAt(index)
                unreadPostsRecycler.adapter?.notifyItemRemoved(index)
                PostsDb.getInstance().markAsRead(post.postId)
            }
        }

        unreadPostsLayout.setOnClickListener {
            val bundle =
                    if (postsOverflow != null) bundleOf("filters" to arrayOf("unread"))
                    else bundleOf()
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.allPostsFromHomeAction, bundle)
        }


        return view

    }
}