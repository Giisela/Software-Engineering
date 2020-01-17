import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import {KolorblindComponent} from './Kolorblind/Kolorblind.component';

const routes: Routes = [
  { path: '', component: KolorblindComponent },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
