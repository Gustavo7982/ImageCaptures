package com.android.imagecaptures.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.imagecaptures.FragmentAux
import com.android.imagecaptures.ImageCaptures
import com.android.imagecaptures.ImageCapturesApplication
import com.android.imagecaptures.R
import com.android.imagecaptures.databinding.FragmentHomeBinding
import com.android.imagecaptures.databinding.ItemImageCapturesBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class HomeFragment : Fragment(), FragmentAux {

    private lateinit var mBinding: FragmentHomeBinding

    private lateinit var mFirebaseAdapter: FirebaseRecyclerAdapter<ImageCaptures, ImageCapturesHolder>
    private lateinit var mLayoutManager: RecyclerView.LayoutManager
    private lateinit var mImageCapturesRef: DatabaseReference

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentHomeBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFirebase()
        setupAdapter()
        setupRecyclerView()
    }

    private fun setupFirebase() {
        mImageCapturesRef = FirebaseDatabase.getInstance().reference.child(ImageCapturesApplication.PATH_SNAPSHOTS)
    }

    private fun setupAdapter() {
        val query = mImageCapturesRef

        val options = FirebaseRecyclerOptions.Builder<ImageCaptures>().setQuery(query) {
            val imageCaptures = it.getValue(ImageCaptures::class.java)
            imageCaptures!!.id = it.key!!
            imageCaptures
        }.build()

        mFirebaseAdapter = object : FirebaseRecyclerAdapter<ImageCaptures, ImageCapturesHolder>(options) {
            private lateinit var mContext: Context

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageCapturesHolder {
                mContext = parent.context

                val view = LayoutInflater.from(mContext)
                        .inflate(R.layout.item_image_captures, parent, false)
                return ImageCapturesHolder(view)
            }

            override fun onBindViewHolder(holder: ImageCapturesHolder, position: Int, model: ImageCaptures) {
                val imageCaptures = getItem(position)

                with(holder) {
                    setListener(imageCaptures)

                    with(binding) {
                        tvTitle.text = imageCaptures.title
                        cbLike.text = imageCaptures.likeList.keys.size.toString()
                        cbLike.isChecked = imageCaptures.likeList
                                .containsKey(ImageCapturesApplication.currentUser.uid)

                        Glide.with(mContext)
                                .load(imageCaptures.photoUrl)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .centerCrop()
                                .into(imgPhoto)
                    }
                }
            }

            @SuppressLint("NotifyDataSetChanged")//error interno firebase ui 8.0.0
            override fun onDataChanged() {
                super.onDataChanged()
                mBinding.progressBar.visibility = View.GONE
                notifyDataSetChanged()
            }

            override fun onError(error: DatabaseError) {
                super.onError(error)
                Snackbar.make(mBinding.root, error.message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        mLayoutManager = LinearLayoutManager(context)

        mBinding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = mLayoutManager
            adapter = mFirebaseAdapter
        }
    }

    override fun onStart() {
        super.onStart()
        mFirebaseAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        mFirebaseAdapter.stopListening()
    }

    private fun deleteImageCaptures(imageCaptures: ImageCaptures) {
        context?.let {
            MaterialAlertDialogBuilder(it)
                    .setTitle(R.string.dialog_delete_title)
                    .setPositiveButton(R.string.dialog_delete_confirm) { _, _ ->
                        //para eliminar la imagen tambien de firebase:
                        val storageImageCapturesRef = FirebaseStorage.getInstance().reference
                            .child(ImageCapturesApplication.PATH_SNAPSHOTS)
                            .child(ImageCapturesApplication.currentUser.uid)
                            .child(imageCaptures.id)
                        storageImageCapturesRef.delete().addOnCompleteListener { result ->
                            if (result.isSuccessful){
                                mImageCapturesRef.child(imageCaptures.id).removeValue()
                            } else {
                                Snackbar.make(mBinding.root, getString(R.string.home_delete_photo_error),
                                    Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }
                    .setNegativeButton(R.string.dialog_delete_cancel, null)
                    .show()
        }
    }

    private fun setLike(imageCaptures: ImageCaptures, checked: Boolean) {
        val myUserRef = mImageCapturesRef.child(imageCaptures.id)
                .child(ImageCapturesApplication.PROPERTY_LIKE_LIST)
                .child(ImageCapturesApplication.currentUser.uid)

        if (checked) {
            myUserRef.setValue(checked)
        } else {
            myUserRef.setValue(null)
        }
    }

    override fun refresh() {
        mBinding.recyclerView.smoothScrollToPosition(0)
    }

    inner class ImageCapturesHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemImageCapturesBinding.bind(view)

        fun setListener(imageCaptures: ImageCaptures) {
            with(binding) {
                btnDelete.setOnClickListener { deleteImageCaptures(imageCaptures) }

                cbLike.setOnCheckedChangeListener { _, checked ->
                    setLike(imageCaptures, checked)
                }
            }
        }
    }
}