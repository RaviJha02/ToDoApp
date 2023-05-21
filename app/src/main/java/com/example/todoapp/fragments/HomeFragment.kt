package com.example.todoapp.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todoapp.R
import com.example.todoapp.databinding.FragmentHomeBinding
import com.example.todoapp.databinding.FragmentSignUpBinding
import com.example.todoapp.utils.ToDoAdapter
import com.example.todoapp.utils.ToDoData
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase

class HomeFragment : Fragment(), AddToDoPopupFragment.OnDialogNextBtnClickListener,
    ToDoAdapter.ToDoAdapterClicksInterface {

    private val TAG = "HomeFragment"
    private lateinit var binding: FragmentHomeBinding
    private lateinit var databaseRef: DatabaseReference
    private var popUpFragment: AddToDoPopupFragment? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var navControl: NavController
    private lateinit var authId: String

    private lateinit var adapter: ToDoAdapter
    private lateinit var mList: MutableList<ToDoData>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init(view)
        getDataFromFirebase()
        registerEvents()
        //get data from firebase
//        getTaskFromFirebase()

//        binding.addTaskBtn.setOnClickListener {
//
//            if (frag != null)
//                childFragmentManager.beginTransaction().remove(frag!!).commit()
//            frag = ToDoDialogFragment()
//            frag!!.setListener(this)
//
//            frag!!.show(
//                childFragmentManager,
//                ToDoDialogFragment.TAG
//            )
//        }
    }

    private fun registerEvents() {
        binding.addBtnHome.setOnClickListener {
            if (popUpFragment != null) {
                childFragmentManager.beginTransaction().remove(popUpFragment!!).commit()
            }
            popUpFragment = AddToDoPopupFragment()
            popUpFragment!!.setListener(this)
            popUpFragment!!.show(
                childFragmentManager,
                AddToDoPopupFragment.TAG
            )
        }
    }

    private fun init(view: View) {

        navControl = Navigation.findNavController(view)
        auth = FirebaseAuth.getInstance()
        authId = auth.currentUser!!.uid
        databaseRef =
            FirebaseDatabase.getInstance().reference.child("Tasks").child(authId.toString())

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        mList = mutableListOf()
        adapter = ToDoAdapter(mList)
        adapter.setListener(this)
        binding.recyclerView.adapter = adapter

    }

    private fun getDataFromFirebase() {
        databaseRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                mList.clear()
                for (taskSnapshot in snapshot.children) {
                    val todoTask =
                        taskSnapshot.key?.let { ToDoData(it, taskSnapshot.value.toString()) }

                    if (todoTask != null) {
                        mList.add(todoTask)
                    }

                }
                Log.d(TAG, "onDataChange: " + mList)
                adapter.notifyDataSetChanged()

            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onSaveTask(todo: String, todoEt: TextInputEditText) {
        databaseRef.push().setValue(todo).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(context, "Task Added Successfully", Toast.LENGTH_SHORT).show()

            } else {
                Toast.makeText(context, it.exception.toString(), Toast.LENGTH_SHORT).show()
            }
            todoEt.text = null
            popUpFragment!!.dismiss()
        }
    }

    override fun onUpdateTask(toDoData: ToDoData, todoEt: TextInputEditText) {
        val map = HashMap<String,Any>()
        map[toDoData.taskId] = toDoData.task
        databaseRef.updateChildren(map).addOnCompleteListener {
            if (it.isSuccessful){
                Toast.makeText(context, "Task Updated Successfully", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(context, it.exception.toString(), Toast.LENGTH_SHORT).show()
            }
            todoEt.text = null
            popUpFragment!!.dismiss()
        }
    }

    override fun onDeleteTaskBtnClicked(toDoData: ToDoData) {
        databaseRef.child(toDoData.taskId).removeValue().addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(context, "Deleted Successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, it.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onEditTaskBtnClicked(toDoData: ToDoData) {
        if (popUpFragment != null){
            childFragmentManager.beginTransaction().remove(popUpFragment!!).commit()
        }
        popUpFragment = AddToDoPopupFragment.newInstance(toDoData.taskId, toDoData.task)
        popUpFragment!!.setListener(this)
        popUpFragment!!.show(childFragmentManager,AddToDoPopupFragment.TAG)
    }

}