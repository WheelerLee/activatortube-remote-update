/**
 * APP更新相关的内容
 * Created at 2020年05月21日10:14:31
 *
 * @author Wheeler https://github.com/WheelerLee
 * @copyright 2019 Activatortube, INC.
 *
 */
import { NativeModules, Platform } from 'react-native';
import Axios from 'axios';

export interface ATVersion {
  /**
   * 版本号
   */
  version_code: number;
  /**
   * 版本名
   */
  version_name: string;
  /**
   * 是否静默安装
   */
  silence: boolean;
  /**
   * 是否强制更新
   */
  force_update: boolean;
  /**
   * 是否到市场更新
   */
  store_update: boolean;
  /**
   * 平台
   */
  platform: string;
  /**
   * 更新日志
   */
  content: string;
  /**
   * 新版本的下载url
   */
  url: string;
}

const NativeATUpdateChecker = NativeModules['ATUpdateChecker'];

export default class ATUpdateChecker {

  /**
   * 检查当前APP是否有新版本，版本号以及平台会自动传递，如果需要其他的参数，使用otherParams传递
   * @param url 检查是否有新版本的url
   * @param otherParams 检查更新需要传递的其他参数
   */
  static check(url: string, otherParams?: any): Promise<ATVersion> {
    
    return new Promise(async function(resolve, reject) {
      try {
        let params = Object.assign({
          version_code: await ATUpdateChecker.getVersionCode(),
          platform: Platform.OS.toLowerCase()
        }, otherParams);
        let res = await Axios.post(url, params);
        if (res.status === 200 && res.data.errCode === 0) {
          resolve(res.data.data);
        } else {
          reject(new Error());
        }
      } catch (error) {
        reject(error);
      }
    });

  }

  /**
   * 获取当前的版本号，如果存在bundle的版本号大于app的版本，将会使用bundle作为版本号
   */
  static getVersionCode(): Promise<number> {
    return NativeATUpdateChecker.getVersionCode();
  }

  /**
   * 下载并且解压bundle到本地，
   * @param url 下载的url
   * @param version_code 当前的版本号
   * @return Promise<string> bundle保存的路径
   */
  static downloadBundle(url: string, version_code: number): Promise<string> {
    return NativeATUpdateChecker.downloadBundle(url, version_code);
  }

}