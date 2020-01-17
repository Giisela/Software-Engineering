import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

import { FormsModule }   from '@angular/forms';
import { HttpModule }    from '@angular/http';

import {KolorblindComponent} from './Kolorblind/Kolorblind.component';
import {KolorblindService} from './Kolorblind/Kolorblind.service';

import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {MatInputModule, MatFormFieldModule, MatTableModule,MatNativeDateModule, MatPaginatorModule,MatToolbarModule,MatCardModule,MatListModule, MatGridListModule,MatIconModule} from '@angular/material';
import { CommonModule } from "@angular/common";


@NgModule({
  declarations: [
    AppComponent,
    KolorblindComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpModule,
    FormsModule,
    BrowserAnimationsModule,
    MatInputModule,
    MatFormFieldModule,
    MatTableModule,
    MatNativeDateModule,
    MatPaginatorModule,
    MatToolbarModule,
    MatCardModule,
    MatListModule,
    MatGridListModule,
    CommonModule,
    MatIconModule
  ],
  providers: [KolorblindService],
  bootstrap: [AppComponent]
})
export class AppModule { }




