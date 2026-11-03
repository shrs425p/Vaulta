package com.vaulta.shrs.navigation

import androidx.lifecycle.ViewModel
import com.vaulta.vault.session.VaultSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavViewModel @Inject constructor(
    val vaultSession: VaultSession
) : ViewModel()
