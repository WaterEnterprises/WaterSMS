package jv.watersms.enterprises.di

import android.content.Context
import jv.watersms.enterprises.data.AppDatabase
import jv.watersms.enterprises.data.CampaignDao
import jv.watersms.enterprises.data.CampaignRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    @Provides
    fun provideCampaignDao(database: AppDatabase): CampaignDao = database.campaignDao()

    @Provides
    @Singleton
    fun provideCampaignRepository(dao: CampaignDao): CampaignRepository = CampaignRepository(dao)
}
