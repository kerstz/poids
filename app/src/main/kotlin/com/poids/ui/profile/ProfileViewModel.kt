package com.poids.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poids.domain.model.ActivityLevel
import com.poids.domain.model.GoalType
import com.poids.domain.model.Sex
import com.poids.domain.model.UserProfile
import com.poids.domain.model.WeightEntry
import com.poids.domain.repository.ProfileRepository
import com.poids.domain.repository.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** État du formulaire de profil. Les nombres sont des chaînes pour gérer la saisie. */
data class ProfileFormState(
    val heightCm: String = "",
    val weightKg: String = "",
    val birthDate: LocalDate? = null,
    val sex: Sex = Sex.MALE,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val goalType: GoalType = GoalType.LOSE,
    val targetWeightKg: String = "",
    val weeklyRateKg: String = "0.5",
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val weightRepository: WeightRepository,
) : ViewModel() {

    private val _form = MutableStateFlow(ProfileFormState())
    val form: StateFlow<ProfileFormState> = _form.asStateFlow()

    /** Vrai si on édite un profil existant (pas d'ajout de pesée initiale dans ce cas). */
    private var isEditing = false

    init {
        viewModelScope.launch {
            profileRepository.getProfile()?.let { p ->
                isEditing = true
                _form.update {
                    it.copy(
                        heightCm = trimNumber(p.heightCm),
                        weightKg = trimNumber(p.weightKg),
                        birthDate = p.birthDate,
                        sex = p.sex,
                        activityLevel = p.activityLevel,
                        goalType = p.goalType,
                        targetWeightKg = p.targetWeightKg?.let(::trimNumber) ?: "",
                        weeklyRateKg = trimNumber(p.weeklyRateKg),
                    )
                }
            }
        }
    }

    fun onHeightChange(v: String) = _form.update { it.copy(heightCm = v, error = null) }
    fun onWeightChange(v: String) = _form.update { it.copy(weightKg = v, error = null) }
    fun onBirthDateChange(v: LocalDate) = _form.update { it.copy(birthDate = v, error = null) }
    fun onSexChange(v: Sex) = _form.update { it.copy(sex = v, error = null) }
    fun onActivityChange(v: ActivityLevel) = _form.update { it.copy(activityLevel = v, error = null) }
    fun onGoalChange(v: GoalType) = _form.update { it.copy(goalType = v, error = null) }
    fun onTargetChange(v: String) = _form.update { it.copy(targetWeightKg = v, error = null) }
    fun onRateChange(v: String) = _form.update { it.copy(weeklyRateKg = v, error = null) }

    fun save() {
        val s = _form.value
        val height = s.heightCm.toFrenchDouble()
        val weight = s.weightKg.toFrenchDouble()
        val target = s.targetWeightKg.toFrenchDouble()
        val rate = s.weeklyRateKg.toFrenchDouble() ?: 0.5

        when {
            height == null || height !in 80.0..250.0 ->
                return fail("Taille invalide (attendu 80–250 cm).")
            weight == null || weight !in 25.0..400.0 ->
                return fail("Poids invalide (attendu 25–400 kg).")
            s.birthDate == null ->
                return fail("Date de naissance requise.")
            s.birthDate.isAfter(LocalDate.now()) ->
                return fail("Date de naissance dans le futur.")
            s.goalType != GoalType.MAINTAIN && (target == null || target !in 25.0..400.0) ->
                return fail("Poids cible invalide.")
        }

        val profile = UserProfile(
            heightCm = height!!,
            weightKg = weight!!,
            birthDate = s.birthDate!!,
            sex = s.sex,
            activityLevel = s.activityLevel,
            goalType = s.goalType,
            targetWeightKg = if (s.goalType == GoalType.MAINTAIN) null else target,
            weeklyRateKg = if (s.goalType == GoalType.MAINTAIN) 0.0 else rate,
        )

        viewModelScope.launch {
            profileRepository.saveProfile(profile)
            if (!isEditing) {
                weightRepository.add(WeightEntry(date = LocalDate.now(), weightKg = weight))
            }
            _form.update { it.copy(saved = true, error = null) }
        }
    }

    private fun fail(message: String) = _form.update { it.copy(error = message) }
}

/** Parse un nombre français (virgule ou point). */
private fun String.toFrenchDouble(): Double? =
    trim().replace(',', '.').toDoubleOrNull()

/** Affiche un Double sans ".0" superflu. */
private fun trimNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
