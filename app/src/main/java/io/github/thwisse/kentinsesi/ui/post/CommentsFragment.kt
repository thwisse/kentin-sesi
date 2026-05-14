package io.github.thwisse.kentinsesi.ui.post

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.data.model.Comment
import io.github.thwisse.kentinsesi.databinding.FragmentCommentsBinding
import io.github.thwisse.kentinsesi.util.Resource

@AndroidEntryPoint
class CommentsFragment : Fragment(R.layout.fragment_comments) {

    private var _binding: FragmentCommentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostDetailViewModel by viewModels()
    
    private lateinit var commentAdapter: CommentAdapter
    private var currentPostId: String? = null
    private var replyingTo: Comment? = null
    
    // Collapse durumu
    private val expandedCommentIds = mutableSetOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCommentsBinding.bind(view)
        
        // Klavye açıldığında input layout'u yukarı taşı
        val inputLayout = binding.inputLayout
        val basePaddingBottom = inputLayout.paddingBottom
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(inputLayout) { v, insets ->
            val imeInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime())
            val systemBarsInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val bottomPadding = maxOf(imeInsets.bottom, systemBarsInsets.bottom)
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, basePaddingBottom + bottomPadding)
            insets
        }

        // Get postId from arguments
        currentPostId = arguments?.getString("postId")
        if (currentPostId == null) {
            Toast.makeText(requireContext(), getString(R.string.post_id_not_found), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        setupCommentAdapter()
        setupCommentInput()
        setupObservers()
        setupSwipeRefresh()

        // Load post (required for delete permission check - also loads current user)
        viewModel.loadPostById(currentPostId!!)
        
        // Load comments
        viewModel.getComments(currentPostId!!)
    }

    private fun setupCommentAdapter() {
        commentAdapter = CommentAdapter(
            onCommentClick = { comment ->
                enterReplyMode(comment)
            },
            onRepliesToggleClick = { rootComment ->
                toggleCommentExpansion(rootComment)
            },
            onCommentLongClick = { comment ->
                if (!comment.isDeleted) {
                    showDeleteCommentDialog(comment)
                }
            }
        )
        binding.rvComments.adapter = commentAdapter
    }

    private fun setupCommentInput() {
        // Enable/disable send button based on input
        binding.etComment.addTextChangedListener { text ->
            binding.btnSendComment.isEnabled = !text.isNullOrBlank()
        }

        binding.btnSendComment.setOnClickListener {
            val text = binding.etComment.text.toString().trim()
            if (text.isNotEmpty()) {
                if (replyingTo != null) {
                    // Sending reply
                    viewModel.addReply(
                        postId = currentPostId!!,
                        text = text,
                        parentCommentId = replyingTo!!.id,
                        replyToAuthorId = replyingTo!!.authorId,
                        replyToAuthorFullName = replyingTo!!.authorFullName
                    )
                } else {
                    // Sending top-level comment
                    viewModel.addComment(currentPostId!!, text)
                }
                binding.etComment.text.clear()
            }
        }

        binding.btnCancelReply.setOnClickListener {
            cancelReplyMode()
        }
    }

    private fun setupObservers() {
        // Comments observer
        viewModel.commentsState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val allComments = resource.data ?: emptyList()
                    updateCommentList(allComments)
                    binding.swipeRefreshLayout.isRefreshing = false
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message ?: getString(R.string.error), Toast.LENGTH_SHORT).show()
                    binding.swipeRefreshLayout.isRefreshing = false
                }
                is Resource.Loading -> {}
            }
        }

        // Add comment result
        viewModel.addCommentState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(requireContext(), getString(R.string.comment_added), Toast.LENGTH_SHORT).show()
                    hideKeyboard()
                    currentPostId?.let { viewModel.getComments(it) }
                }
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        resource.message ?: getString(R.string.comment_add_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is Resource.Loading -> {}
            }
        }

        // Add reply result
        viewModel.addReplyState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(requireContext(), getString(R.string.reply_added), Toast.LENGTH_SHORT).show()
                    
                    android.util.Log.d("DEBUG_REPLY", "=== REPLY SUCCESS ===")
                    android.util.Log.d("DEBUG_REPLY", "replyingTo: ${replyingTo?.id}, depth: ${replyingTo?.depth}")
                    
                    // Yanıt verilen yorumun tüm parent zincirini otomatik expand et
                    replyingTo?.let { targetComment ->
                        // Yorumları al (henüz yüklenemiş olabilir, o zaman bekle)
                        val allComments = (viewModel.commentsState.value as? Resource.Success)?.data ?: emptyList()
                        android.util.Log.d("DEBUG_REPLY", "Current comments count: ${allComments.size}")
                        android.util.Log.d("DEBUG_REPLY", "expandedCommentIds BEFORE: ${expandedCommentIds.joinToString(", ")}")
                        
                        val commentById = allComments.associateBy { it.id }
                        
                        // Target yorumdan başlayarak root'a kadar tüm parent'ları expand et
                        fun expandParentChain(comment: Comment) {
                            android.util.Log.d("DEBUG_REPLY", "Expanding: ${comment.id}, depth: ${comment.depth}")
                            expandedCommentIds.add(comment.id)
                            val parentId = comment.parentCommentId
                            if (!parentId.isNullOrBlank()) {
                                commentById[parentId]?.let { parentComment ->
                                    expandParentChain(parentComment)
                                } ?: android.util.Log.e("DEBUG_REPLY", "Parent not found: $parentId")
                            }
                        }
                        
                        expandParentChain(targetComment)
                        android.util.Log.d("DEBUG_REPLY", "expandedCommentIds AFTER: ${expandedCommentIds.joinToString(", ")}")
                        android.util.Log.d("DEBUG_REPLY", "Total expanded: ${expandedCommentIds.size}")
                    }
                    
                    cancelReplyMode()
                    hideKeyboard()
                    android.util.Log.d("DEBUG_REPLY", "Calling getComments...")
                    currentPostId?.let { viewModel.getComments(it) }
                }
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        resource.message ?: getString(R.string.reply_add_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is Resource.Loading -> {}
            }
        }

        // Delete comment result
        viewModel.deleteCommentState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(requireContext(), getString(R.string.comment_deleted), Toast.LENGTH_SHORT).show()
                    currentPostId?.let { viewModel.getComments(it) }
                }
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        resource.message ?: getString(R.string.comment_delete_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            currentPostId?.let {
                viewModel.getComments(it)
            }
        }
    }

    private fun enterReplyMode(comment: Comment) {
        replyingTo = comment
        binding.replyBanner.isVisible = true
        val name = comment.authorFullName.ifBlank { comment.authorUsername }
        binding.tvReplyBannerText.text = getString(R.string.replying_to, name)
        binding.etComment.requestFocus()
    }

    private fun cancelReplyMode() {
        replyingTo = null
        binding.replyBanner.isVisible = false
        binding.etComment.text.clear()
    }

    private fun showDeleteCommentDialog(comment: Comment) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_comment_title)
            .setMessage(R.string.dialog_delete_comment_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteComment(comment.id)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.etComment.windowToken, 0)
        binding.etComment.clearFocus()
    }
    
    private fun toggleCommentExpansion(comment: Comment) {
        if (expandedCommentIds.contains(comment.id)) {
            expandedCommentIds.remove(comment.id)
        } else {
            expandedCommentIds.add(comment.id)
        }
        // Refresh görünümünü güncellemek için mevcut listeyi yeniden güncelle
        val currentList = (viewModel.commentsState.value as? Resource.Success)?.data ?: emptyList()
        updateCommentList(currentList)
    }
    
    private fun updateCommentList(allComments: List<Comment>) {
        android.util.Log.d("DEBUG_UPDATE", "=== UPDATE COMMENT LIST ===")
        android.util.Log.d("DEBUG_UPDATE", "Total comments: ${allComments.size}")
        android.util.Log.d("DEBUG_UPDATE", "expandedCommentIds: ${expandedCommentIds.joinToString(", ")}")
        
        // Hangi yorumların kaç çocuğu var hesapla
        val childCountMap = mutableMapOf<String, Int>()
        allComments.forEach { comment ->
            val parentId = comment.parentCommentId
            if (!parentId.isNullOrBlank()) {
                childCountMap[parentId] = (childCountMap[parentId] ?: 0) + 1
            }
        }
        
        android.util.Log.d("DEBUG_UPDATE", "Child counts: ${childCountMap.entries.joinToString { "${it.key}=${it.value}" }}")
        
        // Parent ID'ye göre yorumları grupla (recursive kontrol için)
        val commentById = allComments.associateBy { it.id }
        
        // Bir yorumun tüm parent zincirinin expand edilmiş olup olmadığını kontrol et
        fun isCommentVisible(comment: Comment): Boolean {
            // Ana yorumlar her zaman görünür
            if (comment.parentCommentId.isNullOrBlank()) {
                return true
            }
            
            // Parent'ı bul
            val parent = commentById[comment.parentCommentId]
            if (parent == null) {
                // Parent bulunamazsa gizle (güvenlik)
                android.util.Log.e("DEBUG_UPDATE", "Parent not found for comment ${comment.id}, parentId: ${comment.parentCommentId}")
                return false
            }
            
            // Parent expand edilmemişse bu yorum görünmez
            if (!expandedCommentIds.contains(parent.id)) {
                android.util.Log.d("DEBUG_UPDATE", "Comment ${comment.id} hidden because parent ${parent.id} not expanded")
                return false
            }
            
            // Parent'ın da görünür olup olmadığını kontrol et (recursive)
            return isCommentVisible(parent)
        }
        
        // Collapse durumuna göre listeyi filtrele
        val visibleComments = allComments.filter { comment ->
            val visible = isCommentVisible(comment)
            if (!visible) {
                android.util.Log.d("DEBUG_UPDATE", "Hiding comment: ${comment.id}, depth: ${comment.depth}, parent: ${comment.parentCommentId}")
            }
            visible
        }
        
        android.util.Log.d("DEBUG_UPDATE", "Visible comments: ${visibleComments.size}")
        
        // Adapter'a güncellenmiş verileri gönder
        commentAdapter.setChildCountByParentId(childCountMap)
        commentAdapter.setExpandedCommentIds(expandedCommentIds)
        commentAdapter.submitList(visibleComments)
    }

    override fun onResume() {
        super.onResume()
        // Hide bottom navigation
        (activity as? io.github.thwisse.kentinsesi.ui.MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            io.github.thwisse.kentinsesi.R.id.bottom_nav_view
        )?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        // Show bottom navigation
        (activity as? io.github.thwisse.kentinsesi.ui.MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            io.github.thwisse.kentinsesi.R.id.bottom_nav_view
        )?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
