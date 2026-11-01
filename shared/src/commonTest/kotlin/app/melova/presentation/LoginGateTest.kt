package app.melova.presentation

import app.melova.data.model.LoginMethods
import app.melova.data.model.StudentAccount
import app.melova.data.remote.dto.StudentMeDto
import app.melova.data.repository.toDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginGateTest {
    private val phoneLogin = LoginMethods(phone = true, email = false, googleClientId = null)

    private fun account(signedIn: Boolean, name: String = "", needsName: Boolean = false) = StudentAccount(
        id = "st_1",
        name = name,
        signedIn = signedIn,
        signedInAs = null,
        photoVersion = null,
        isTeacher = false,
        ownedLessons = emptySet(),
        needsName = needsName
    )

    private fun gate(account: StudentAccount?, methods: LoginMethods? = phoneLogin) =
        StudentUiState(account = account, loginMethods = methods).loginGate

    @Test
    fun someoneNotSignedInGoesToLogin() {
        assertEquals(LoginGate.SIGN_IN, gate(account(signedIn = false)))
    }

    @Test
    fun aNewAccountWaitsOnTheNamePage() {
        assertEquals(LoginGate.WAIT, gate(account(signedIn = true, needsName = true)))
    }

    @Test
    fun theWayOpensOnceTheNameIsGiven() {
        assertEquals(LoginGate.OPEN, gate(account(signedIn = true, name = "سارا")))
    }

    @Test
    fun aReturningAccountWithANameGoesStraightOn() {
        assertEquals(LoginGate.OPEN, gate(account(signedIn = true, name = "سارا", needsName = false)))
    }

    /** The held action must not run in the moment between the old account and the new guest. */
    @Test
    fun leavingTheAccountFromTheNamePageNeverOpensTheWay() {
        assertEquals(LoginGate.WAIT, gate(null))
        assertEquals(LoginGate.SIGN_IN, gate(account(signedIn = false)))
    }

    @Test
    fun nothingStopsAGuestWhileTheServerOffersNoLogin() {
        assertEquals(LoginGate.OPEN, gate(account(signedIn = false), methods = LoginMethods(false, false, null)))
    }

    private fun answer(name: String?, signedIn: Boolean = true) =
        StudentMeDto(studentId = "st_1", name = name, signedIn = signedIn, ownedLessons = emptyList())

    @Test
    fun anyAnswerKeepsTheNameQuestionOpenUntilThereIsAName() {
        assertTrue(answer(name = "").toDomain(awaitingName = true).needsName)
        assertFalse(answer(name = "سارا").toDomain(awaitingName = true).needsName)
    }

    @Test
    fun aGuestIsNotAskedTheirName() {
        assertFalse(answer(name = "", signedIn = false).toDomain(awaitingName = true).needsName)
    }

    /** The page asks after a sign-in, so a session already open is left alone. */
    @Test
    fun anAccountThatDidNotJustSignInIsNotAskedTheirName() {
        assertFalse(answer(name = "").toDomain(awaitingName = false).needsName)
    }
}
