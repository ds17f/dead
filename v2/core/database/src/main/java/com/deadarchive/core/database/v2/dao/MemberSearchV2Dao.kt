package com.deadarchive.v2.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.deadarchive.v2.core.database.entities.MemberSearchV2Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberSearchV2Dao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<MemberSearchV2Entity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: MemberSearchV2Entity)
    
    @Query("DELETE FROM member_search_v2")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM member_search_v2 WHERE memberName LIKE '%' || :query || '%' OR memberKey LIKE '%' || :query || '%' OR instruments LIKE '%' || :query || '%' ORDER BY rating DESC, date DESC")
    suspend fun searchMembers(query: String): List<MemberSearchV2Entity>
    
    @Query("SELECT * FROM member_search_v2 WHERE memberKey = :memberKey ORDER BY date DESC")
    suspend fun getShowsForMember(memberKey: String): List<MemberSearchV2Entity>
    
    @Query("SELECT * FROM member_search_v2 GROUP BY memberKey ORDER BY memberName")
    suspend fun getAllUniqueMembers(): List<MemberSearchV2Entity>
    
    @Query("SELECT * FROM member_search_v2 WHERE showId = :showId ORDER BY memberName")
    suspend fun getMembersForShow(showId: String): List<MemberSearchV2Entity>
    
    @Query("SELECT * FROM member_search_v2 WHERE instruments LIKE '%' || :instrument || '%' ORDER BY rating DESC, date DESC")
    suspend fun getMembersByInstrument(instrument: String): List<MemberSearchV2Entity>
    
    @Query("SELECT * FROM member_search_v2 WHERE date BETWEEN :startDate AND :endDate ORDER BY rating DESC")
    suspend fun getMembersInDateRange(startDate: String, endDate: String): List<MemberSearchV2Entity>
    
    @Query("SELECT * FROM member_search_v2 WHERE venue LIKE '%' || :venue || '%' ORDER BY rating DESC, date DESC")
    suspend fun getMembersByVenue(venue: String): List<MemberSearchV2Entity>
    
    @Query("SELECT DISTINCT instruments FROM member_search_v2")
    suspend fun getAllInstruments(): List<String>
    
    @Query("SELECT COUNT(DISTINCT memberKey) FROM member_search_v2")
    suspend fun getUniqueMemberCount(): Int
    
    @Query("SELECT COUNT(*) FROM member_search_v2")
    suspend fun getTotalMemberShowCount(): Int
    
    @Query("SELECT memberKey, memberName, COUNT(*) as showCount FROM member_search_v2 GROUP BY memberKey ORDER BY showCount DESC")
    suspend fun getMemberShowCounts(): List<MemberShowCount>
}

data class MemberShowCount(
    val memberKey: String,
    val memberName: String,
    val showCount: Int
)