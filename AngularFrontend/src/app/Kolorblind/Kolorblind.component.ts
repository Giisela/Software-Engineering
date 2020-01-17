import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { NgForm, FormGroup, FormBuilder, Validators } from '@angular/forms';
import { KolorblindService } from './Kolorblind.service';
import { error } from 'util';
import {Kolorblind} from './Kolorblind'

@Component({
  selector: 'app-Kolorblind',
  templateUrl: './Kolorblind.component.html',
  styleUrls: ['./Kolorblind.component.scss'],
})

export class KolorblindComponent implements OnInit {
  image = new Image();

  constructor(
    private KolorblindService : KolorblindService,
  ) {
  }

  uploadImage(): void{
    this.KolorblindService.uploadImage(this.image)
        .subscribe((Response) => {console.log(Response)} , (error) => {
          console.log(error);
        });
        
  }

  
  
  ngOnInit(): void {
    
  }

  
}