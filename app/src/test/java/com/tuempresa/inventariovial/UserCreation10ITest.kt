package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class UserCreation10ITest {
    @get:Rule val compose=createComposeRule()
    @Test fun compactActionsRemainInsideTheViewportAndKeepTheirCallbacks() {
        var created=0;var synced=0;var audit=0
        compose.setContent {InventarioVialTheme {Box(Modifier.width(260.dp)){UserAdminActions(false,{created++},{synced++},{audit++})}}}
        val bounds=compose.onNodeWithTag("user-admin-actions").fetchSemanticsNode().boundsInRoot
        for(label in listOf("Nuevo usuario","Sincronizar usuarios","Auditoría")) {
            val button=compose.onNodeWithText(label);button.assertIsDisplayed()
            val r=button.fetchSemanticsNode().boundsInRoot;assertTrue(r.left>=bounds.left && r.right<=bounds.right && r.bottom<=bounds.bottom)
            button.performClick()
        }
        assertEquals(1,created);assertEquals(1,synced);assertEquals(1,audit)
    }
    @Test fun operatorCannotCreateAndAdminRequiresOnlineAuthorizationThatExpires()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use {db->
            val sessions=AuthSessionManager();val row=UserCacheEntity("test10i","test10i","Inspector ficticio","OPERATOR",true,"",0,0,0)
            sessions.login(row);var time=10000L;var creates=0;var calls=0
            val service=object:UserService {override suspend fun call(action:String,fields:JSONObject):JSONObject {
                calls++;return when(action){"adminAuthenticate"->JSONObject().put("adminToken","synthetic-10i").put("expiresInSeconds",300)
                    "createUser"->{creates++;assertEquals("synthetic-10i",fields.getString("adminToken"));JSONObject().put("id","new-synthetic-10i")}
                    else->throw UserServiceException("OFFLINE")}
            }}
            val protector=object:CredentialProtector {override fun encrypt(plaintext:String,purpose:String)=plaintext;override fun decrypt(ciphertext:String,purpose:String)=ciphertext}
            val sync=UserSyncRepository(db,service,protector,sessions)
            val admin=AdminUsersRepository(db,service,sessions,sync,{true},{time})
            assertTrue(runCatching {admin.mutate("createUser",JSONObject())}.isFailure);assertEquals(0,calls)
            sessions.login(row.copy(role="ADMIN"))
            assertEquals("SESSION_EXPIRED",(runCatching {admin.mutate("createUser",JSONObject())}.exceptionOrNull() as UserServiceException).code)
            admin.authenticate("123456".toCharArray())
            // More than twenty cached users do not impose a creation cap in Android.
            repeat(25){db.userDao().upsertUsers(listOf(row.copy(id="synthetic-$it",username="synthetic$it")))}
            val message=admin.mutate("createUser",JSONObject().put("username","newtest10i").put("displayName","Ficticio").put("role","OPERATOR"))
            assertTrue(message.contains("Falta actualizar",ignoreCase=true));assertEquals(1,creates)
            time+=300001
            assertEquals("SESSION_EXPIRED",(runCatching {admin.mutate("createUser",JSONObject())}.exceptionOrNull() as UserServiceException).code)
            assertEquals(1,creates)
        }
    }
}
