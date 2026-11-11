package app.melova.presentation

import app.melova.data.model.LoginChannel
import app.melova.data.remote.dto.LoginMethodsDto
import app.melova.data.repository.toDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginMethodsTest {
    private fun methods(mock: Boolean?, mockKinds: List<String>?) =
        LoginMethodsDto(phone = true, email = true, googleClientId = null, mock = mock, mockKinds = mockKinds)
            .toDomain(canUseGoogle = true)

    @Test
    fun emailAsksForACodeWhilePhoneStillSignsInWithout() {
        val methods = methods(mock = false, mockKinds = listOf("phone"))
        assertTrue(methods.isMock(LoginChannel.PHONE))
        assertFalse(methods.isMock(LoginChannel.EMAIL))
    }

    @Test
    fun aServerFromBeforeMockKindsMocksEveryChannelOrNone() {
        assertEquals(LoginChannel.entries.toSet(), methods(mock = true, mockKinds = null).mockChannels)
        assertEquals(emptySet(), methods(mock = false, mockKinds = null).mockChannels)
    }

    @Test
    fun googleHidesWhereThisPlatformCanNotUseIt() {
        val dto = LoginMethodsDto(phone = false, email = false, googleClientId = "web-client", mock = null)
        assertEquals("web-client", dto.toDomain(canUseGoogle = true).googleClientId)
        assertEquals(null, dto.toDomain(canUseGoogle = false).googleClientId)
    }
}
