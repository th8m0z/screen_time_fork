//
//  ScreenTimeMethod.swift
//  Pods
//
//  Created by Chandra Abdul Fattah on 29/04/25.
//

import FamilyControls

class ScreenTimeMethod {
    static func requestPermission(
        type: ScreenTimePermissionType = ScreenTimePermissionType.appUsage
    ) async -> Bool {
        switch type {
            case ScreenTimePermissionType.appUsage:
                do {
                    try await AuthorizationCenter.shared.requestAuthorization(for: FamilyControlsMember.individual)
                    print("Request Permission Launched")
                    return true
                } catch {
                    print("Request Permission Failed: \(error.localizedDescription)")
                    return false
                }
            default:
                return true
        }
    }
    
    static func permissionStatus(type: ScreenTimePermissionType = ScreenTimePermissionType.appUsage) async -> ScreenTimePermissionStatus {
        switch type {
            case ScreenTimePermissionType.appUsage:
                let status = AuthorizationCenter.shared.authorizationStatus
                let statusEnum = status == .approved ? ScreenTimePermissionStatus.approved :
                    status == .denied ? ScreenTimePermissionStatus.denied : ScreenTimePermissionStatus.notDetermined
                return statusEnum
            default:
                return ScreenTimePermissionStatus.approved
        }
    }
}
