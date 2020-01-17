import { Injectable } from '@angular/core';
import { Headers, Http } from '@angular/http';
import {Kolorblind} from './Kolorblind';
//import 'rxjs/add/operator/toPromise';

@Injectable()
export class KolorblindService {

  private baseUrl = 'http://localhost:8080';

  constructor(private http: Http) { }

  uploadImage(image: Kolorblind){
    return this.http.post(this.baseUrl + '/api/image/upload', image);
  }

    private handleError(error: any): Promise<any> {
    console.error('Some error occured', error);
    return Promise.reject(error.message || error);
  }
  
}