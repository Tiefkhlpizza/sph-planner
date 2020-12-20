package de.koenidv.sph.objects

import java.net.URL
import java.util.*

//  Created by koenidv on 07.12.2020.
data class PostLink(
        var id_course: String, // Course to be attached to, i.e. m_bar_48 (48 is sph post id)
        var id_post: String, // Post to be attached to, i.e. m_bar_1_post-2020-12-07_48
        var name: String?, // Could be used after resolving
        var date: Date, // Date of the post to be attached to. Should be day only, no time
        var url: URL // Url that has been found
)