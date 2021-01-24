/*
 * Created by Evgeniya Zemlyanaya (@zzemlyanaya)
 * Copyright (c) 2021 . All rights reserved.
 * Last modified 24.01.2021, 12:56
 */

package ru.zzemlyanaya.tfood.main

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import ru.zzemlyanaya.tfood.LOGOUT
import ru.zzemlyanaya.tfood.R
import ru.zzemlyanaya.tfood.data.local.LocalRepository
import ru.zzemlyanaya.tfood.data.local.PrefsConst
import ru.zzemlyanaya.tfood.data.remote.RemoteRepository
import ru.zzemlyanaya.tfood.databinding.ActivityMainBinding
import ru.zzemlyanaya.tfood.login.LoginActivity
import ru.zzemlyanaya.tfood.main.basicquiz.BasicFragment
import ru.zzemlyanaya.tfood.main.basicquiz.BasicResultFragment
import ru.zzemlyanaya.tfood.main.dairy.DairyFragment
import ru.zzemlyanaya.tfood.main.dashboard.DashboardFragment
import ru.zzemlyanaya.tfood.main.product.ProductFragment
import ru.zzemlyanaya.tfood.main.product.ProductSearchFragment
import ru.zzemlyanaya.tfood.main.profile.ProfileFragment
import ru.zzemlyanaya.tfood.main.sleepquiz.SleepQuizFragment
import ru.zzemlyanaya.tfood.main.statistics.StatisticsFragment
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val localRepository = LocalRepository.getInstance()
    private val remoteRepository = RemoteRepository()
    private lateinit var token: String
    private lateinit var id: String

    private var backPressedOnce = false

    private lateinit var myFabSrc: Drawable

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.frame_main)
        when(fragment!!.tag) {
            "dashboard", "dairy", "statistics", "profile" -> onBackPressedDouble()
            "settings", "about_app", "shop", "achiev" -> showProfile()
            "add_meal" -> showDairy()
            "add_new_product" -> super.onBackPressed()
            else -> {}
        }
    }

    private fun onBackPressedDouble() {
        if (backPressedOnce)
            logout()
        else {
            backPressedOnce = true
            Toast.makeText(
                this@MainActivity,
                "Нажмите ещё раз для выхода",
                Toast.LENGTH_SHORT
            ).show()
            Handler().postDelayed({ backPressedOnce = false }, 2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomBarNav.apply{
            onItemSelectedListener = { _, menuItem ->
                when (menuItem.itemId) {
                    R.id.item_home -> showDashboard()
                    R.id.item_dairy -> showDairy()
                    R.id.item_profile -> showProfile()
                    R.id.item_statistics -> showStatistics()
                }
            }
            onItemReselectedListener = { _, _ -> }
        }


        myFabSrc = getDrawable(R.drawable.ic_add_black)!!

        val firstOverall = localRepository.getPref(PrefsConst.FIELD_IS_FIRST_LAUNCH) as Boolean
        token = localRepository.getPref(PrefsConst.FIELD_USER_TOKEN) as String
        id = localRepository.getPref(PrefsConst.FIELD_USER_ID) as String

        val lastSleepDate = localRepository.getPref(PrefsConst.FIELD_LAST_SLEEP_DATE) as String

        val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

        if (token == "" || firstOverall) {
            showBasicQuiz(false)
            localRepository.updatePref(PrefsConst.FIELD_IS_FIRST_LAUNCH, false)
        }
        else if (lastSleepDate != today) {
            showSleepQuiz(true)
            localRepository.updatePref(PrefsConst.FIELD_LAST_SLEEP_DATE, today)
        }
        else
            showDashboard()

        setUpFabs()
        binding.fab.setOnClickListener {
            val tag = supportFragmentManager.findFragmentById(R.id.frame_main)?.tag
            if (tag == "dashboard" || tag == "dairy") {
                if (View.GONE == binding.fabGroup.visibility)
                    showFABMenu()
                else
                    closeFABMenu()
            }
        }

    }

    private fun showBasicQuiz(shouldSendData: Boolean){
        binding.bottomBarNav.visibility = View.GONE
        binding.fab.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(R.id.frame_main, BasicFragment.newInstance(shouldSendData, id), "basic_quiz")
            .commitAllowingStateLoss()
    }

    fun showDashboard(){
        binding.bottomBarNav.visibility = View.VISIBLE
        binding.fab.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.frame_main, DashboardFragment(), "dashboard")
            .commitAllowingStateLoss()
    }

    fun showSleepQuiz(shouldSendOnlySleep: Boolean){
        binding.bottomBarNav.visibility = View.GONE
        binding.fab.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(
                R.id.frame_main,
                SleepQuizFragment.newInstance(shouldSendOnlySleep, token),
                "sleep_quiz"
            )
            .commitAllowingStateLoss()
    }

    fun showBasicResult(weightVal: Int, border: Double, kcalNorm: Int, waterNorm: Int){
        val weight = (localRepository.getPref(PrefsConst.FIELD_USER_DATA) as String).split(';')[3].toIntOrNull()
        supportFragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(
                    R.id.frame_main, BasicResultFragment.newInstance(
                        weightVal,
                        border,
                        weight ?: 0,
                        kcalNorm,
                        waterNorm
                    ), "basic_result"
                )
                .commitAllowingStateLoss()
    }

    fun showDairy() {
        binding.fab.visibility = View.VISIBLE
        binding.bottomBarNav.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.frame_main, DairyFragment(), "dairy")
            .commitAllowingStateLoss()
    }

    private fun setUpFabs(){
        binding.fabDinner.setOnClickListener { showAddMeal(R.string.dinner) }
        binding.fabLunch.setOnClickListener { showAddMeal(R.string.lunch) }
        binding.fabBreakfast.setOnClickListener { showAddMeal(R.string.breakfast) }
        binding.fabSnack.setOnClickListener { showAddMeal(R.string.snack) }
    }

    private fun showFABMenu() {
        binding.fabGroup.visibility = View.VISIBLE
        val willBeOrange = myFabSrc.constantState!!.newDrawable()
        willBeOrange.mutate().setColorFilter(getColor(R.color.primaryColour), PorterDuff.Mode.SRC_ATOP)
        binding.fab.apply {
            backgroundTintList = ColorStateList.valueOf(getColor(R.color.white))
            setImageDrawable(willBeOrange)
        }
        binding.backShadow.animate().alpha(0.45f)
        binding.fab.animate().rotationBy(45F)
        binding.fabDinnerLayout.animate().alpha(1F)
        binding.fabLunchLayout.animate().alpha(1F)
        binding.fabBreakfastLayout.animate().alpha(1F)
        binding.fabSnackLayout.animate().alpha(1F)
        binding.fabSportLayout.animate().alpha(1F)
        binding.fabChoresLayout.animate().alpha(1F)
    }

    private fun closeFABMenu() {
        binding.fab.animate().rotation(0f)
        val willBeWhite = myFabSrc.constantState!!.newDrawable()
        willBeWhite.mutate().setColorFilter(getColor(R.color.white), PorterDuff.Mode.SRC_ATOP)
        binding.fab.apply {
            backgroundTintList = ColorStateList.valueOf(getColor(R.color.primaryColour))
            setImageDrawable(willBeWhite)
        }
        binding.backShadow.animate().alpha(0f)
        binding.fabDinnerLayout.animate().alpha(0f)
        binding.fabLunchLayout.animate().alpha(0f)
        binding.fabBreakfastLayout.animate().alpha(0f)
        binding.fabSnackLayout.animate().alpha(0f)
        binding.fabSportLayout.animate().alpha(0f)
        binding.fabChoresLayout.animate().alpha(0f)
        binding.fabGroup.visibility = View.GONE
    }

    fun showStatistics() {
        binding.fab.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.frame_main, StatisticsFragment(), "statistics")
            .commitAllowingStateLoss()
    }

    fun showProfile() {
        binding.fab.visibility = View.GONE
        supportFragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.frame_main, ProfileFragment(), "profile")
                .commitAllowingStateLoss()
    }

    fun showAddMeal(meal_res: Int){
        closeFABMenu()
        binding.fab.visibility = View.GONE
        binding.bottomBarNav.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.frame_main, ProductSearchFragment.newInstance(getString(meal_res)), "add_meal")
            .commitAllowingStateLoss()
    }

    fun showAddNewProduct(){
//        supportFragmentManager.beginTransaction()
//                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
//                .add(R.id.frame_main, AddNewProductFragment(), "add_new_product")
//                .commitAllowingStateLoss()
    }

    fun showProductInfo(id: String) {
        supportFragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.frame_main, ProductFragment.newInstance(id), "product_info")
                .commitAllowingStateLoss()
    }

    fun logout(){
        //remoteRepository.logout(getStandardHeader(token))
        localRepository.apply {
            updatePref(PrefsConst.FIELD_USER_DATA, "Username;2006-1-1;180;65;85")
            updatePref(PrefsConst.FIELD_MACRO_NOW, "0;0;0;0;0")
            updatePref(PrefsConst.FIELD_USER_NOW, "0;0")
            updatePref(PrefsConst.FIELD_USER_ID, "")
            updatePref(PrefsConst.FIELD_USER_TOKEN, "")
            updatePref(PrefsConst.FIELD_SLEEP_TODAY, 0)
            updatePref(PrefsConst.FIELD_LAST_SLEEP_DATE, "")
        }
        val intent = Intent(this, LoginActivity::class.java).apply {
            putExtra(LOGOUT, true)
        }
        startActivity(intent)
        finish()
    }
}